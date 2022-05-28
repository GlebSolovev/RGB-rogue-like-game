package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.simple

import ru.hse.sd.rgb.gamelogic.engines.behaviour.SimpleBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.State
import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.engines.fight.AttackType
import ru.hse.sd.rgb.gamelogic.engines.fight.ControlParams
import ru.hse.sd.rgb.gamelogic.engines.fight.HealType
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.*
import ru.hse.sd.rgb.utils.structures.Paths2D

class PassiveBehaviour(entity: GameEntity, movePeriodMillis: Long) : SimpleBehaviour(entity) {

    private val moveTicker = Ticker(movePeriodMillis, entity, MoveTick()).also { it.start() }

    override var state: State = object : State() {

        private val pathStrategy = Paths2D.randomWalk()

        override suspend fun handleReceivedAttack(message: ReceivedAttack): State {
            if (message.isFatal) controller.creation.die(entity)
            return this
        }

        override suspend fun handleCollidedWith(message: CollidedWith): State {
            controller.fighting.attack(message.myUnit, message.otherUnit)
            return this
        }

        override suspend fun handleColorTick(tick: ColorTick): State {
            controller.fighting.update(tick.unit, ControlParams(AttackType.RANDOM_TARGET, HealType.RANDOM_TARGET))
            return this
        }

        override suspend fun handleMoveTick(): State {
            val cell = entity.units.first().cell
            val moved = controller.physics.tryMove(entity, pathStrategy.next(cell))
            if (moved) controller.view.receive(EntityUpdated(entity))
            return this
        }

    }

    override fun stopTickers() {
        moveTicker.stop()
    }

}