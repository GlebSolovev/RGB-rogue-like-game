package ru.hse.sd.rgb.logic

import kotlinx.coroutines.sync.Mutex
import ru.hse.sd.rgb.Cell
import ru.hse.sd.rgb.Direction
import ru.hse.sd.rgb.entities.common.GameEntity
import ru.hse.sd.rgb.entities.common.GameUnit
import ru.hse.sd.rgb.get
import ru.hse.sd.rgb.plus

class PhysicsLogic(val h: Int, val w: Int) {

    private val worldGrid = List(h) { List(w) { mutableSetOf<GameUnit>() } }

    private val worldGridLocks = List(h) { List(w) { Mutex() } }

    private suspend inline fun <R> withLockedArea(area: Set<Cell>, crossinline block: () -> R): R {
        val sortedArea = area.toSortedSet(Comparator.comparing(Cell::x).thenComparing(Cell::y))
        for (cell in sortedArea) worldGridLocks[cell].lock()
        val result = block()
        for (cell in sortedArea.reversed()) worldGridLocks[cell].unlock()
        return result
    }

    private fun isInBounds(c: Cell) = (c.x in 0 until w) && (c.y in 0 until h)

    private fun checkAvailability(cells: Set<Cell>) =
        cells.flatMap { worldGrid[it] }.firstOrNull { it.parent.physicalEntity.isSolid || isInBounds(it.cell) } == null

    suspend fun tryMove(entity: GameEntity, dir: Direction): Boolean {
        val units = entity.units
        val cells = units.map { it.cell }.toSet()
        val nextCells = entity.units.map { it.cell + dir.toShift() }.toSet()
        if (nextCells.any { !isInBounds(it) }) return false
        return withLockedArea(cells union nextCells) {
            if (!checkAvailability(nextCells subtract cells)) return@withLockedArea false
            units.forEach {
                worldGrid[it.cell].remove(it)
                it.cell += dir.toShift()
                worldGrid[it.cell].add(it)
            }
            return@withLockedArea true
        }
    }

    suspend fun tryPopulate(entity: GameEntity): Boolean {
        val units = entity.units
        val cells = units.map { it.cell }.toSet()
        return withLockedArea(cells) {
            if(!checkAvailability(cells)) return@withLockedArea false
            units.forEach {
                worldGrid[it.cell].add(it)
            }
            return@withLockedArea true
        }
    }

}