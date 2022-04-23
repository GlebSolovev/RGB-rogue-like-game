package ru.hse.sd.rgb.logic

import kotlinx.coroutines.sync.Mutex
import ru.hse.sd.rgb.Cell
import ru.hse.sd.rgb.Direction
import ru.hse.sd.rgb.entities.common.CollidedWith
import ru.hse.sd.rgb.entities.common.GameEntity
import ru.hse.sd.rgb.entities.common.GameUnit
import ru.hse.sd.rgb.get
import ru.hse.sd.rgb.plus
import kotlin.random.Random

class PhysicsLogic(val h: Int, val w: Int) {

    companion object {
        const val RANDOM_TARGET_ATTEMPTS = 1000000
    }

    private val worldGrid = List(h) { List(w) { mutableSetOf<GameUnit>() } } // TODO: check concurrency

    private val worldGridLocks = List(h) { List(w) { Mutex() } }

    // take mutexes on cells not units!!!
    private suspend inline fun <R> withLockedArea(area: Set<Cell>, crossinline block: () -> R): R {
        val sortedArea = area.toSortedSet(Comparator.comparing(Cell::x).thenComparing(Cell::y))
        try {
            for (cell in sortedArea) worldGridLocks[cell].lock()
            return block()
        } finally {
            for (cell in sortedArea.reversed()) worldGridLocks[cell].unlock()
        }
    }

    private fun isInBounds(c: Cell) = (c.x in 0 until w) && (c.y in 0 until h)

    private fun checkAvailability(physicalEntity: GameEntity.PhysicalEntity, nextCells: Set<Cell>): Boolean {
        if (nextCells.any { !isInBounds(it) }) return false
        val units = nextCells.flatMap { worldGrid[it] }
        if (physicalEntity.isSolid) return units.isEmpty()
        return !units.any { it.parent.physicalEntity.isSolid }
    }

    suspend fun tryMove(entity: GameEntity, dir: Direction): Boolean {
        val units = entity.units
        val cells = units.map { it.cell }.toSet()
        val unitsShifts = units.associateWith { entity.physicalEntity.getUnitDirection(it, dir).toShift() }
        val nextCells = entity.units.map { it.cell + unitsShifts[it]!! }.toSet()
        if (nextCells.any { !isInBounds(it) }) return false
        return withLockedArea(cells union nextCells) {
            if (!checkAvailability(entity.physicalEntity, nextCells subtract cells)) return@withLockedArea false
            units.forEach { myUnit ->
                worldGrid[myUnit.cell].remove(myUnit)
                myUnit.cell += unitsShifts[myUnit]!!
                for (otherUnit in worldGrid[myUnit.cell]) {
                    otherUnit.parent.receive(CollidedWith(otherUnit, myUnit))
                    entity.receive(CollidedWith(myUnit, otherUnit))
                }
                worldGrid[myUnit.cell].add(myUnit)
            }
            return@withLockedArea true
        }
    }

    suspend fun tryPopulate(entity: GameEntity): Boolean {
        val units = entity.units
        val cells = units.map { it.cell }.toSet()
        return withLockedArea(cells) {
            if (!checkAvailability(entity.physicalEntity, cells)) return@withLockedArea false
            units.forEach {
                worldGrid[it.cell].add(it)
            }
            return@withLockedArea true
        }
    }

    suspend fun deleteUnit(unit: GameUnit) {
        withLockedArea(setOf(unit.cell)) {
            worldGrid[unit.cell].remove(unit)
        }
    }

    // won't hit entity instantly
    fun generateRandomTarget(entity: GameEntity, random: Random = Random): Cell {
        val entityCells = entity.units.map { it.cell }.toSet()
        repeat(RANDOM_TARGET_ATTEMPTS) {
            val cell = Cell(random.nextInt(w), random.nextInt(h))
            if(!entityCells.contains(cell)) return cell
        }
        throw IllegalStateException("$RANDOM_TARGET_ATTEMPTS attempts exceeded")
    }

}