package ru.hse.sd.rgb.entities

import ru.hse.sd.rgb.*
import ru.hse.sd.rgb.Ticker.Companion.Ticker
import ru.hse.sd.rgb.entities.common.*
import ru.hse.sd.rgb.views.EntityMoved
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape
import kotlin.math.abs

class Fireball(
    colorCell: ColorCell,
    private val movePeriodMillis: Long,
    private val targetCell: Cell
) : GameEntity(setOf(colorCell)) {

    val ticker = Ticker(movePeriodMillis, MoveTick()).also { it.start() }

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit): ViewUnit = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.CIRCLE)
        }
    }

    override val physicalEntity = object : PhysicalEntity() {
        override val isSolid = false
        override fun getUnitDirection(unit: GameUnit, dir: Direction): Direction = dir
    }

    override suspend fun handleGameMessage(m: Message) {
        when (m) {
            is MoveTick -> {
                val cell = units.first().cell
                if (cell == targetCell) { // TODO: continue flying
                    controller.creation.tryDie(this)
                    return
                }
                val dx = targetCell.x - cell.x
                val dy = targetCell.y - cell.y
                val dir = if (abs(dx) > abs(dy)) {
                    if (dx > 0) Direction.RIGHT else Direction.LEFT
                } else {
                    if (dy > 0) Direction.DOWN else Direction.UP
                }
                val moved = controller.physics.tryMove(this, dir)
                if (moved) controller.view.receive(EntityMoved(this))
            }
            is CollidedWith -> {
                controller.fighting.attack(m.myUnit, m.otherUnit)
                controller.creation.tryDie(this)
            }
            is ColorTick -> ignore
            else -> unreachable
        }
    }
}