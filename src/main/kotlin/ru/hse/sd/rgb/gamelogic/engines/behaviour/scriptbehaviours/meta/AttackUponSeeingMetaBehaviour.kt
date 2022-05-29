package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.MetaBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.State
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.simple.DirectAttackBehaviour
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.Ticker
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.messages.WatcherTick
import ru.hse.sd.rgb.utils.randomCell
import ru.hse.sd.rgb.utils.structures.Paths2D

class AttackUponSeeingMetaBehaviour(
    initialBehaviour: Behaviour,
    entity: GameEntity,
    private val targetEntity: GameEntity,
    private val seeingDepth: Int,
    directAttackMovePeriodMillis: Long,
    watchPeriodMillis: Long
) : MetaBehaviour(initialBehaviour, entity) {

    private val watcherTicker = Ticker(watchPeriodMillis, entity, WatcherTick())

    override var metaState: State = NotSeeingTargetState()
    private var behaviour: Behaviour = initialBehaviour

    private val attackBehaviour = DirectAttackBehaviour(entity, directAttackMovePeriodMillis, targetEntity)

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
            return controller.physics.checkPathAvailability(
                pathStrategy,
                startCell,
                seeingDepth,
                { !it.parent.physicalEntity.isSolid },
                { it.parent == targetEntity })
        }
    }

    private inner class SeeingTargetState : BaseState() {
        override suspend fun handleWatcherTick(): State {
            return if (isSeeingTarget()) {
                this
            } else {
                behaviour.stopTickers()
                behaviour = initialBehaviour
                behaviour.startTickers()
                NotSeeingTargetState()
            }
        }
    }

    private inner class NotSeeingTargetState : BaseState() {
        override suspend fun handleWatcherTick(): State {
            return if (isSeeingTarget()) {
                behaviour.stopTickers()
                behaviour = attackBehaviour
                behaviour.startTickers()
                SeeingTargetState()
            } else {
                this
            }
        }
    }

    override fun startTickers() {
        watcherTicker.start()
        behaviour.startTickers()
    }

    override fun stopTickers() {
        watcherTicker.stop()
        behaviour.stopTickers()
    }
}