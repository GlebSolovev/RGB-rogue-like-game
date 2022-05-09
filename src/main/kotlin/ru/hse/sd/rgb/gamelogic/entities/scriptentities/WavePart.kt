package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.entities.*
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.views.EntityUpdated
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape
import ru.hse.sd.rgb.utils.Ticker.Companion.Ticker

class WavePart(
    cell: ColorCellNoHp,
    movePeriodMillis: Long,
    private val dir: Direction
) : GameEntity(setOf(cell)) {

    val moveTicker = Ticker(movePeriodMillis, MoveTick()).also { it.start() }

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit) = object : ViewUnit(unit) {
            override val swingAppearance: SwingUnitAppearance
                get() = SwingUnitAppearance(SwingUnitShape.CIRCLE_HALF(dir))
        }
    }

    override val physicalEntity = object : PhysicalEntity() {
        override val isSolid = false
        override fun getUnitDirection(unit: GameUnit, dir: Direction) = dir
    }

    override val fightEntity = object : FightEntity() {
        override fun isUnitActive(unit: GameUnit) = false
    }

    override suspend fun handleGameMessage(m: Message) {
        when (m) {
            is MoveTick -> {
                val moved = controller.physics.tryMove(this, dir)
                if (moved) controller.view.receive(EntityUpdated(this))
            }
            is CollidedWith -> {
                controller.fighting.attack(m.myUnit, m.otherUnit)
                controller.creation.die(this)
            }
            is ReceivedAttack -> if (m.isFatal) controller.creation.die(this)
            is ColorTick -> ignore
            else -> unreachable
        }
    }

}