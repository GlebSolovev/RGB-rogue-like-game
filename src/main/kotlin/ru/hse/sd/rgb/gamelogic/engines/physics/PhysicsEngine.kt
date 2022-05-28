package ru.hse.sd.rgb.gamelogic.engines.physics

import kotlinx.coroutines.sync.Mutex
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.utils.messaging.messages.CollidedWith
import ru.hse.sd.rgb.utils.structures.Grid2D
import ru.hse.sd.rgb.utils.structures.Paths2D
import kotlin.random.Random

class PhysicsEngine(private val w: Int, private val h: Int) {

    companion object {
        const val RANDOM_TARGET_ATTEMPTS = 1000000
    }

    private val worldGrid = Grid2D(w, h) { _, _ -> mutableSetOf<GameUnit>() } // TODO: check concurrency

    private val worldGridLocks = Grid2D(w, h) { _, _ -> Mutex() }

    // take mutexes on cells not units!!!
    private suspend inline fun <R> withLockedArea(area: Set<Cell>, crossinline block: suspend () -> R): R {
        val sortedArea = area.toSortedSet(Comparator.comparing(Cell::x).thenComparing(Cell::y))
        val lockedMutexes = mutableListOf<Mutex>()
        try {
            for (cell in sortedArea) {
                val mutex = worldGridLocks[cell]
                mutex.lock()
                lockedMutexes.add(mutex)
            }
            return block()
        } finally {
            for (mutex in lockedMutexes.reversed()) mutex.unlock()
        }
    }

    private fun isInBounds(c: Cell) = (c.x in 0 until w) && (c.y in 0 until h)

    private fun checkAvailability(
        physicalEntity: GameEntity.PhysicalEntity,
        nextCellsForUnits: Map<GameUnit, Cell>
    ): Map<GameUnit, Set<GameUnit>> {
        val incompatibleUnitsMap = mutableMapOf<GameUnit, Set<GameUnit>>()

        for ((unit, nextCell) in nextCellsForUnits) {
            if (!isInBounds(nextCell)) {
                incompatibleUnitsMap[unit] = setOf()
                continue
            }
            val nextCellUnits = worldGrid[nextCell]

            val incompatibleUnits = physicalEntity.filterIncompatibleUnits(physicalEntity, nextCellUnits)
            if (incompatibleUnits.isNotEmpty()) incompatibleUnitsMap[unit] = incompatibleUnits
        }
        return incompatibleUnitsMap
    }

    private fun sendCollidedWith(myUnit: GameUnit, otherUnit: GameUnit) {
        otherUnit.parent.receive(CollidedWith(otherUnit, myUnit))
        myUnit.parent.receive(CollidedWith(myUnit, otherUnit))
    }

    // sends CollidedWith to all next cells units
    suspend fun tryMove(
        entity: GameEntity,
        dir: Direction
    ): Boolean {
        val units = entity.units
        val cells = units.map { it.cell }.toSet()
        val unitsShifts = units.associateWith { entity.physicalEntity.getUnitDirection(it, dir).toShift() }
        val nextCells = entity.units.map { it.cell + unitsShifts[it]!! }.toSet()
        if (nextCells.any { !isInBounds(it) }) return false
        val nextCellsForEdgeUnits = (nextCells subtract cells).let { nextEdgeCells ->
            unitsShifts
                .filter { (unit, _) -> (unit.cell + unitsShifts[unit]!!) in nextEdgeCells }
                .mapValues { (unit, gridShift) -> unit.cell + gridShift }
        }

        return withLockedArea(cells union nextCells) {
            for (myUnit in units)
                for (otherUnit in worldGrid[myUnit.cell + unitsShifts[myUnit]!!])
                    if (otherUnit.parent != entity) sendCollidedWith(myUnit, otherUnit)

            if (checkAvailability(
                    entity.physicalEntity,
                    nextCellsForEdgeUnits
                ).isNotEmpty()
            ) return@withLockedArea false

            units.forEach { myUnit ->
                worldGrid[myUnit.cell].remove(myUnit)
                myUnit.cell += unitsShifts[myUnit]!!
                // TODO in line above: reuse unitsShifts (operator `+` for cell and direction?)
                myUnit.lastMoveDir = entity.physicalEntity.getUnitDirection(myUnit, dir)
                worldGrid[myUnit.cell].add(myUnit)
            }
            true
        }
    }

    // if true, sends CollidedWith to all units
    suspend fun tryPopulate(entity: GameEntity): Boolean {
        val units = entity.units
        val cellsWithUnits = units.associateWith { it.cell }
        if (cellsWithUnits.values.any { !isInBounds(it) }) return false

        return withLockedArea(cellsWithUnits.values.toSet()) {
            if (checkAvailability(entity.physicalEntity, cellsWithUnits).isNotEmpty()) return@withLockedArea false
            units.forEach { worldGrid[it.cell].add(it) }
            units.forEach { myUnit ->
                for (otherUnit in worldGrid[myUnit.cell])
                    if (otherUnit.parent != entity) sendCollidedWith(myUnit, otherUnit)
            }
            // TODO: fix FightEngine possible exception: one tries to attack collided but he is not there
            return@withLockedArea true
        }
    }

    suspend fun remove(entity: GameEntity) {
        val units = entity.units
        val cells = units.map { it.cell }.toSet()
        withLockedArea(cells) {
            units.forEach { worldGrid[it.cell].remove(it) }
        }
    }

    // won't hit entity instantly
    fun generateRandomTarget(entity: GameEntity, random: Random = Random): Cell {
        val entityCells = entity.units.map { it.cell }.toSet()
        repeat(RANDOM_TARGET_ATTEMPTS) {
            val cell = Cell(random.nextInt(w), random.nextInt(h))
            if (!entityCells.contains(cell)) return cell
        }
        throw IllegalStateException("$RANDOM_TARGET_ATTEMPTS attempts exceeded")
    }

    suspend fun checkPathAvailability(
        path: Paths2D.PathStrategy,
        startCell: Cell,
        depth: Int,
        unitIsOk: (GameUnit) -> Boolean
    ): Boolean {
        val pathCells = generatePathCells(path, startCell, depth)
        return withLockedArea(pathCells) {
            pathCells.all { worldGrid[it].all(unitIsOk) }
        }
    }

    private fun generatePathCells(path: Paths2D.PathStrategy, startCell: Cell, depth: Int): Set<Cell> {
        val pathCells = mutableSetOf<Cell>()
        var currentCell = startCell
        repeat(depth) {
            if (!isInBounds(currentCell)) return pathCells
            pathCells.add(currentCell)
            currentCell += path.next(currentCell).toShift()
        }
        return pathCells
    }

}
