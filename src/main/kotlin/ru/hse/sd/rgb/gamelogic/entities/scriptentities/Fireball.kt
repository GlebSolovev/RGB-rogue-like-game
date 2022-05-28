package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.State
import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.simple.MoveSimpleBehaviour
import ru.hse.sd.rgb.gamelogic.entities.*
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.utils.messaging.messages.*
import ru.hse.sd.rgb.utils.structures.Paths2D
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class Fireball(
    colorCell: ColorCellNoHp,
    movePeriodMillis: Long,
    targetCell: Cell,
    teamId: Int,
) : GameEntity(setOf(colorCell)) {

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

    override var behaviour: Behaviour = FireballDefaultBehaviour(colorCell, movePeriodMillis, targetCell)
    override val behaviourEntity = SingleBehaviourEntity(behaviour)

    private inner class FireballDefaultBehaviour(
        colorCell: ColorCellNoHp,
        movePeriodMillis: Long,
        targetCell: Cell
    ) : MoveSimpleBehaviour(this, movePeriodMillis) {

        private val pathStrategy = Paths2D.straightLine(colorCell.cell, targetCell)

        override var state = object : State() {

            override suspend fun handleReceivedAttack(message: ReceivedAttack): State {
                if (message.isFatal) controller.creation.die(this@Fireball)
                return this
            }

            override suspend fun handleCollidedWith(message: CollidedWith): State {
                if (message.otherUnit.parent.fightEntity.teamId == fightEntity.teamId) return this
                controller.fighting.attack(message.myUnit, message.otherUnit)
                controller.creation.die(this@Fireball)
                return this
            }

            override suspend fun handleColorTick(tick: ColorTick): State = this

            override suspend fun handleMoveTick(): State {
                val cell = units.first().cell
                val moved = controller.physics.tryMove(this@Fireball, pathStrategy.next(cell))
                if (moved) controller.view.receive(EntityUpdated(this@Fireball))
                return this
            }
        }
    }
}