package ru.hse.sd.rgb.gamelogic.engines.physics

import kotlinx.coroutines.sync.Mutex
import ru.hse.sd.rgb.gamelogic.entities.CollidedWith
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.*
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
        try {
            for (cell in sortedArea) worldGridLocks[cell].lock()
            return block()
        } finally {
            for (cell in sortedArea.reversed()) worldGridLocks[cell].unlock()
        }
    }

    private fun isInBounds(c: Cell) = (c.x in 0 until w) && (c.y in 0 until h)

    private fun checkAvailability(
        physicalEntity: GameEntity.PhysicalEntity,
        nextCellsForUnits: Map<GameUnit, Cell>
    ): Map<GameUnit, Set<GameUnit>> {
        val collidedUnits = mutableMapOf<GameUnit, Set<GameUnit>>()

        for ((unit, nextCell) in nextCellsForUnits) {
            if (!isInBounds(nextCell)) {
                collidedUnits[unit] = setOf()
                continue
            }
            val nextCellUnits = worldGrid[nextCell]

            if (physicalEntity.isSolid && nextCellUnits.isNotEmpty()) {
                collidedUnits[unit] = nextCellUnits
            } else if (nextCellUnits.any { it.parent.physicalEntity.isSolid }) {
                if (nextCellUnits.size != 1) throw IllegalStateException("more than one solid entity on same cell")
                collidedUnits[unit] = nextCellUnits
            }
        }
        return collidedUnits
    }

    private fun sendCollidedWith(myUnit: GameUnit, otherUnit: GameUnit) {
        otherUnit.parent.receive(CollidedWith(otherUnit, myUnit))
        myUnit.parent.receive(CollidedWith(myUnit, otherUnit))
    }

    suspend fun tryMove(entity: GameEntity, dir: Direction): Boolean {
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
            val collidedUnits = checkAvailability(entity.physicalEntity, nextCellsForEdgeUnits)
            if (collidedUnits.isNotEmpty()) {
                for ((myUnit, nextCellUnits) in collidedUnits)
                    for (otherUnit in nextCellUnits) sendCollidedWith(myUnit, otherUnit)
                return@withLockedArea false
            }
            units.forEach { myUnit ->
                worldGrid[myUnit.cell].remove(myUnit)
                myUnit.cell += unitsShifts[myUnit]!!
                for (otherUnit in worldGrid[myUnit.cell]) sendCollidedWith(myUnit, otherUnit)
                worldGrid[myUnit.cell].add(myUnit)
            }
            return@withLockedArea true
        }
    }

    suspend fun tryPopulate(entity: GameEntity): Boolean {
        val units = entity.units
        val cellsWithUnits = units.associateWith { it.cell }
        return withLockedArea(cellsWithUnits.values.toSet()) {
            if (checkAvailability(entity.physicalEntity, cellsWithUnits).isNotEmpty()) return@withLockedArea false
            units.forEach {
                worldGrid[it.cell].add(it)
            }
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

    fun tryExpand(entity: GameEntity, newUnits: Set<GameUnit>): Boolean {
        TODO()
    }

    suspend fun deleteUnit(unit: GameUnit) {
        TODO()
//        withLockedArea(setOf(unit.cell)) {
//            worldGrid[unit.cell].remove(unit)
//        }
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

}