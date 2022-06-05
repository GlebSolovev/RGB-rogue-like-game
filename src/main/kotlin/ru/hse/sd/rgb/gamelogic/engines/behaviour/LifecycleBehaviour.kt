package ru.hse.sd.rgb.gamelogic.engines.behaviour

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.ignore
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.Tick
import ru.hse.sd.rgb.utils.messaging.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.*
import ru.hse.sd.rgb.utils.notAllowed
import ru.hse.sd.rgb.utils.unreachable
import kotlin.reflect.KClass

class LifecycleBehaviour(
    entity: GameEntity,
    private var childBehaviour: Behaviour
) : Behaviour(entity) {

    private enum class LifecycleState { NOT_STARTED, ONGOING, DEAD }

    private var lifeCycleState: LifecycleState = LifecycleState.NOT_STARTED

    override suspend fun handleMessage(message: Message) {
        when (lifeCycleState) {
            LifecycleState.NOT_STARTED -> {
                when (message) {
                    is LifeStarted -> {
                        lifeCycleState = LifecycleState.ONGOING
                        controller.view.receive(EntityUpdated(entity))
                        entity.onLifeStart()
                        childBehaviour.startSubtreeTickers()
                    }
                    is LifeEnded -> lifeCycleState = LifecycleState.DEAD
                    else -> ignore
                }
            }
            LifecycleState.ONGOING -> {
                when (message) {
                    is LifeStarted -> unreachable
                    is LifeEnded -> {
                        lifeCycleState = LifecycleState.DEAD
                        message.dieRoutine()
                        controller.view.receive(EntityRemoved(entity))
                        entity.onLifeEnd()
                        childBehaviour.stopSubtreeTickers()
                    }
                    is ApplyBehaviourMessage -> {
                        childBehaviour = message.createNewBehaviour(childBehaviour)
                        entity.viewEntity.applyMessageToAppearance(message)
                    }
                    is RemoveBehaviourMessage -> {
                        childBehaviour.traverseSubtree {
                            val itChild = (it as? MetaBehaviour)?.childBehaviour
                            if (itChild === message.target) {
                                it.childBehaviour = itChild.childBehaviour
                                itChild.stopTickers()
                            }
                        }
                        entity.viewEntity.applyMessageToAppearance(message)
                    }
                    else -> {
                        childBehaviour.handleMessage(message)
                        entity.viewEntity.applyMessageToAppearance(message)
                    }
                }
            }
            LifecycleState.DEAD -> {
                when (message) {
                    is LifeStarted -> unreachable
                    else -> ignore
                }
            }
        }
    }

    override fun tickersGroup(tickClass: KClass<out Tick>) = notAllowed
    override fun traverseTickers(onEach: (Ticker) -> Unit) = notAllowed
    override fun traverseSubtree(onEach: (Behaviour) -> Unit) = notAllowed
}
