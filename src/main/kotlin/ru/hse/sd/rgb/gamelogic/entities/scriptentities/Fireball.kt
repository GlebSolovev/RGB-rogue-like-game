package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.utils.Ticker.Companion.createTicker
import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.entities.*
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.views.EntityUpdated
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class Fireball(
    colorCell: ColorCellNoHp,
    movePeriodMillis: Long,
    targetCell: Cell,
    teamId: Int,
) : GameEntity(setOf(colorCell)) {

    val ticker = createTicker(movePeriodMillis, MoveTick()).also { it.start() }
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

    override val fightEntity = object : FightEntity() {
        override fun isUnitActive(unit: GameUnit): Boolean = false
        override val teamId = teamId
    }

    private val pathStrategy = Paths2D.straightLine(colorCell.cell, targetCell)

    override suspend fun handleGameMessage(m: Message) {
        when (m) {
            is MoveTick -> {
                val cell = units.first().cell
                val moved = controller.physics.tryMove(this, pathStrategy.next(cell))
                if (moved) controller.view.receive(EntityUpdated(this))
            }
            is CollidedWith -> {
                if (m.otherUnit.parent.fightEntity.teamId == this.fightEntity.teamId) return
                controller.fighting.attack(m.myUnit, m.otherUnit)
                controller.creation.die(this)
            }
            is ReceivedAttack -> if (m.isFatal) controller.creation.die(this)
            is ColorTick -> ignore
            else -> unreachable
        }
    }
}