package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.entities.*
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.utils.Ticker.Companion.Ticker
import ru.hse.sd.rgb.views.EntityUpdated
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape
import kotlin.random.Random

// single-unit (because entities are indivisible)
class Glitch(cell: Cell, hp: Int) : GameEntity(setOf(ColorHpCell(RGB(0, 0, 0), hp, cell))) {

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit) = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.SQUARE)
        }
    }

    override val physicalEntity = object : PhysicalEntity() {
        override val isSolid = false

        override fun getUnitDirection(unit: GameUnit, dir: Direction): Direction = dir
    }

    private class RepaintTick() : Tick()

    private val repaintTick = RepaintTick()
    private val repaintTicker = Ticker(10, repaintTick).also { it.start() }
    private val moveTicker = Ticker(5000, MoveTick()).also { it.start() }

    private val random = Random

    override suspend fun handleGameMessage(m: Message): Unit = when (m) {
        is RepaintTick -> Unit.also {
            val newUnits = units.map { GameUnit(this, it.cell, it.hp, generateRandomColor(random)) }
            units.clear()
            units.addAll(newUnits)
            controller.view.receive(EntityUpdated(this))
        }
        is MoveTick -> Unit.also {
            val cells = units.map { it.cell }.toSet()
            val adjacentCells =
                cells.flatMap { cell -> Direction.values().map { cell + it.toShift() } }.toSet() subtract cells
            val targetCell = adjacentCells.randomElement(random)!!
            if (targetCell !in units.map { it.cell }.toSet()) {
                val clone = clone(targetCell)
                if (controller.creation.tryAddToWorld(clone)) {
                    controller.view.receive(EntityUpdated(clone))
                }
            }
        }
        // TODO
        is CollidedWith -> ignore
        is ColorTick -> ignore
        is ReceivedAttack -> ignore
        else -> unreachable(m)
    }

    private fun clone(targetCell: Cell): Glitch {
        return Glitch(targetCell, units.first().hp)
    }

}