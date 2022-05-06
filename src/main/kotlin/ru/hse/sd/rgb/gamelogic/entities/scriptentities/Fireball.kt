package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.utils.Ticker.Companion.Ticker
import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.entities.*
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.views.EntityUpdated
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape
import kotlin.math.abs

class Fireball(
    colorHpCell: ColorHpCell,
    movePeriodMillis: Long,
    private val targetCell: Cell
) : GameEntity(setOf(colorHpCell)) {

    val ticker = Ticker(movePeriodMillis, MoveTick()).also { it.start() }
    // TODO: possible update ticker via effects

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
                    controller.creation.die(this)
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
                if (moved) controller.view.receive(EntityUpdated(this))
            }
            is CollidedWith -> {
                controller.fighting.attack(m.myUnit, m.otherUnit)
                controller.creation.die(this)
            }
            is ColorTick -> ignore
            is ReceivedAttack -> {
                // TODO
            }
            else -> unreachable
        }
    }
}