package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.simple

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.State
import ru.hse.sd.rgb.gamelogic.engines.fight.AttackType
import ru.hse.sd.rgb.gamelogic.engines.fight.ControlParams
import ru.hse.sd.rgb.gamelogic.engines.fight.HealType
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.messages.*
import ru.hse.sd.rgb.utils.randomCell
import ru.hse.sd.rgb.utils.structures.Paths2D

class DirectAttackBehaviour(
    entity: GameEntity,
    movePeriodMillis: Long,
    targetEntity: GameEntity
) : MoveSimpleBehaviour(entity, movePeriodMillis) {

    override var state: State = object : State() {

        override suspend fun handleReceivedAttack(message: ReceivedAttack): State {
            if (message.isFatal) controller.creation.die(entity)
            return this
        }

        override suspend fun handleCollidedWith(message: CollidedWith): State {
            controller.fighting.attack(message.myUnit, message.otherUnit)
            return this
        }

        override suspend fun handleColorTick(tick: ColorTick): State {
            controller.fighting.update(tick.unit, ControlParams(AttackType.HERO_TARGET, HealType.LOWEST_HP_TARGET))
            return this
        }

        override suspend fun handleMoveTick(): State {
            val pathStrategy = Paths2D.straightLine(entity.units.first().cell, targetEntity.randomCell())
            val cell = entity.units.first().cell
            val moved = controller.physics.tryMove(entity, pathStrategy.next(cell))
            if (moved) controller.view.receive(EntityUpdated(entity))
            return this
        }

    }
}