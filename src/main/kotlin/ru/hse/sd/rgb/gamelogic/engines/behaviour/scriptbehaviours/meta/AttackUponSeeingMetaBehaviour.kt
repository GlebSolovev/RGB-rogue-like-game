package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.MetaBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.State
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.simple.DirectAttackBehaviour
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.Ticker
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.messages.MoveTick
import ru.hse.sd.rgb.utils.messaging.messages.WatcherTick
import ru.hse.sd.rgb.utils.randomCell
import ru.hse.sd.rgb.utils.structures.Paths2D

class AttackUponSeeingMetaBehaviour(
    initialBehaviour: Behaviour,
    entity: GameEntity,
    private val targetEntity: GameEntity,
    private val seeingDepth: Int,
    private val directAttackMovePeriodMillis: Long,
    watchPeriodMillis: Long
) : MetaBehaviour(initialBehaviour, entity) {

    private val watcherTicker = Ticker(watchPeriodMillis, entity, MoveTick()).also { it.start() }

    override var metaState: State = NotSeeingTargetState()
    private var behaviour: Behaviour = initialBehaviour

    private abstract inner class BaseState : State() {

        override suspend fun next(message: Message): State {
            return if (message is WatcherTick) {
                handleWatcherTick()
            } else {
                behaviour.handleMessage(message)
                this
            }
        }

        abstract override suspend fun handleWatcherTick(): State

        protected suspend fun isSeeingTarget(): Boolean {
            val startCell = entity.units.first().cell
            val pathStrategy = Paths2D.straightLine(startCell, targetEntity.randomCell())
            return controller.physics.checkPathAvailability(pathStrategy, startCell, seeingDepth) {
                !it.parent.physicalEntity.isSolid
            }
        }
    }

    private inner class SeeingTargetState : BaseState() {
        override suspend fun handleWatcherTick(): State {
            return if (isSeeingTarget()) {
                this
            } else {
                behaviour = initialBehaviour
                NotSeeingTargetState()
            }
        }
    }

    private inner class NotSeeingTargetState : BaseState() {
        override suspend fun handleWatcherTick(): State {
            return if (isSeeingTarget()) {
                behaviour = DirectAttackBehaviour(entity, directAttackMovePeriodMillis, targetEntity)
                SeeingTargetState()
            } else {
                this
            }
        }
    }

    override fun stopTickers() {
        watcherTicker.stop()
        behaviour.stopTickers()
    }
}