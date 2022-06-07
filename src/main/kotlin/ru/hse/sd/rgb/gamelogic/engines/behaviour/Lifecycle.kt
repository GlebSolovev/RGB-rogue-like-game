package ru.hse.sd.rgb.gamelogic.engines.behaviour

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.ignore
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.messages.*
import ru.hse.sd.rgb.utils.unreachable

class Lifecycle(
    private val entity: GameEntity,
    private var childBehaviour: Behaviour
) {
    private enum class LifecycleState { NOT_STARTED, ONGOING, DEAD }

    private var lifeCycleState: LifecycleState = LifecycleState.NOT_STARTED

    suspend fun handleMessage(message: Message) {
        when (lifeCycleState) {
            LifecycleState.NOT_STARTED -> {
                when (message) {
                    is LifeStarted -> {
                        lifeCycleState = LifecycleState.ONGOING
                        controller.view.receive(EntityUpdated(entity))
                        entity.onLifeStart()
                        childBehaviour.startSubtree()
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
                        childBehaviour.stopSubtree()
                    }
                    is ApplyBehaviourMessage -> {
                        childBehaviour = message.createNewBehaviour(childBehaviour)
                        childBehaviour.start()
                        entity.viewEntity.applyMessageToAppearance(message)
                    }
                    is RemoveBehaviourMessage -> {
                        val child = childBehaviour
                        if (child === message.target) {
                            childBehaviour = child.childBehaviour
                            child.stop()
                        } else {
                            childBehaviour.traverseSubtree {
                                val itChild = (it as? MetaBehaviour)?.childBehaviour
                                if (itChild === message.target) {
                                    it.childBehaviour = itChild.childBehaviour
                                    itChild.stop()
                                }
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
}
