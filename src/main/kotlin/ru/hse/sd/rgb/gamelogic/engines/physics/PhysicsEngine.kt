package ru.hse.sd.rgb.gamelogic.engines.physics

import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.messaging.messages.CollidedWith
import ru.hse.sd.rgb.utils.structures.*
import kotlinx.coroutines.sync.Mutex
import kotlin.random.Random

/**
 * Class for calculating and performing physical operations with entities.
 *
 * All entities exist on a 2D grid of their units, which represents a single level.
 *
 * The supported operations are performing movements, detecting collisions, checking
 * if paths are obstructed and so on.
 *
 * @param w Width of level.
 * @param h Height of level.
 * @constructor Creates a PhysicsEngine with no entities on the grid.
 */
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

            val incompatibleUnits = physicalEntity.filterIncompatibleUnits(nextCellUnits)
            if (incompatibleUnits.isNotEmpty()) incompatibleUnitsMap[unit] = incompatibleUnits
        }
        return incompatibleUnitsMap
    }

    private fun sendCollidedWith(myUnit: GameUnit, otherUnit: GameUnit) {
        otherUnit.parent.receive(CollidedWith(otherUnit, myUnit))
        myUnit.parent.receive(CollidedWith(myUnit, otherUnit))
    }

    /**
     * Tries to move an existing [GameEntity] in the specified [Direction] by one [Cell].
     *
     * The [entity] is allowed to move if and only if after such move all its units would end up:
     *  1) in bounds of the grid
     *  2) on cells that don't already contain any incompatible cells as defined by
     *  entity's [GameEntity.PhysicalEntity].
     *
     *  If both conditions are satisfied, all of [entity]'s units are moved. It is guaranteed that
     *  during the move all cells involved will not be changed in any way.
     *
     *  In any case, [entity] will also receive a [CollidedWith] message for each its unit that
     *  collided with another unit (that is, a unit which either ended up or would have ended up
     *  on a cell, where there are units of another [GameEntity]). There will be a message for
     *  each pair of collided units. Another entity will receive a similar [CollidedWith] message
     *  as well.
     *
     *  @param entity The entity to move. Must have been added to this PhysicsEngine grid
     *  beforehand, otherwise behaviour is undefined.
     *  @param dir Direction to move the entity in.
     *
     *  @return true if the entity was moved, false otherwise.
     */
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

    /**
     * Tries to add the [entity] to this PhysicsEngine grid.
     *
     * The entity is allowed to be added if and only if none of its units would have
     * ended up on an incompatible cell as defined by [GameEntity.PhysicalEntity].
     *
     * If the entity is added, it will receive a [CollidedWith] message for each unit
     * that ended up on a cell that contains units of other entities. There will be a
     * message for each pair of units. Other entities will receive a similar [CollidedWith]
     * message as well.
     *
     * @param entity The entity to add.
     * @return true if the entity was added, false otherwise.
     */
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

    /**
     * Removes the [entity] from this PhysicsEngine grid.
     *
     * This method always succeeds and returns nothing. If the entity was not added
     * to this PhysicsEngine before, behaviour is undefined.
     */
    suspend fun remove(entity: GameEntity) {
        val units = entity.units
        val cells = units.map { it.cell }.toSet()
        withLockedArea(cells) {
            units.forEach { worldGrid[it.cell].remove(it) }
        }
    }

    // won't hit entity instantly
    /**
     * Generate a random cell in bounds of this grid which is not occupied by [entity].
     *
     * This method can be used by an entity to use a random target for an attack and
     * not attack itself.
     *
     * @throws IllegalStateException If too many attempts were used.
     *
     * @param entity The entity which cells are excluded.
     * @param random Random number generator. Optional.
     */
    fun generateRandomTarget(entity: GameEntity, random: Random = Random): Cell {
        val entityCells = entity.units.map { it.cell }.toSet()
        repeat(RANDOM_TARGET_ATTEMPTS) {
            val cell = Cell(random.nextInt(w), random.nextInt(h))
            if (!entityCells.contains(cell)) return cell
        }
        throw IllegalStateException("$RANDOM_TARGET_ATTEMPTS attempts exceeded")
    }

    /**
     * Checks if all cells on [path] are available.
     *
     * This method will iterate through all cells of the [path] in normal order
     * and then run [unitIsOk] predicate for each unit found on that cell. If any
     * of these runs fail, the path is considered not available.
     *
     * Additionally, [unitIsTarget] is run for each unit on the path. If this predicate
     * is satisfied, the iteration stops and the path is considered available.
     *
     * If the iteration goes on for [depth] steps and all [unitIsOk] are satisfied to that
     * point, the iteration stops and the path is considered available.
     *
     * @param path The path to test.
     * @param startCell The cell to start iterating [path] from.
     * @param depth The maximum number of steps to be taken along the [path].
     * @param unitIsOk The predicate that decides whether a unit can appear on an
     * available path.
     * @param unitIsTarget The predicate that decides whether iteration can be stopped
     * upon encountering a unit.
     * @return true if [path] is considered available, false otherwise.
     */
    suspend fun checkPathAvailability(
        path: Paths2D.PathStrategy,
        startCell: Cell,
        depth: Int,
        unitIsOk: (GameUnit) -> Boolean,
        unitIsTarget: (GameUnit) -> Boolean
    ): Boolean {
        val pathCells = generatePathCells(path, startCell, depth)
        return withLockedArea(pathCells) {
            pathCells.all { worldGrid[it].all(unitIsOk) }
            for (cell in pathCells) {
                if (!worldGrid[cell].all(unitIsOk)) return@withLockedArea false
                if (worldGrid[cell].any(unitIsTarget)) return@withLockedArea true
            }
            false
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
