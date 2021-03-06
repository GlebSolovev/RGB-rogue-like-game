package ru.hse.sd.rgb.gamelogic.engines.behaviour

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.ignore
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.messages.*
import ru.hse.sd.rgb.utils.unreachable
import java.util.*

class Lifecycle(
    private val entity: GameEntity,
    private var childBehaviour: Behaviour
) {
    private enum class LifecycleState { NOT_STARTED, ONGOING, DEAD }

    private var lifeCycleState: LifecycleState = LifecycleState.NOT_STARTED

    private val saveInNotStartedAndReplayInOngoingMessages: Queue<SaveInNotStartedAndReplayInOngoingMessage> =
        LinkedList()

    suspend fun handleMessage(message: Message) {
        when (lifeCycleState) {
            LifecycleState.NOT_STARTED -> handleMessageInNotStarted(message)
            LifecycleState.ONGOING -> handleMessageInOngoing(message)
            LifecycleState.DEAD -> ignore
        }
    }

    private suspend fun handleMessageInNotStarted(message: Message) {
        when (message) {
            is LifeStarted -> {
                lifeCycleState = LifecycleState.ONGOING
                controller.view.receive(EntityUpdated(entity))
                entity.onLifeStart()
                childBehaviour.startSubtree()
                while (saveInNotStartedAndReplayInOngoingMessages.isNotEmpty()) {
                    val savedMessage = saveInNotStartedAndReplayInOngoingMessages.remove()
                    handleMessage(savedMessage)
                }
            }
            is LifeEnded -> lifeCycleState = LifecycleState.DEAD
            else -> {
                if (message is SaveInNotStartedAndReplayInOngoingMessage)
                    saveInNotStartedAndReplayInOngoingMessages.add(message)
            }
        }
    }

    private suspend fun handleMessageInOngoing(message: Message) {
        when (message) {
            is LifeStarted -> unreachable
            is LifeEnded -> {
                lifeCycleState = LifecycleState.DEAD
                message.dieRoutine()
                controller.view.receive(EntityRemoved(entity))
                entity.onLifeEnd()
                childBehaviour.stopSubtree()
            }
            is ApplyBehaviourMessage -> handleApplyBehaviourMessage(message)
            is RemoveBehaviourMessage -> handleRemoveBehaviourMessage(message)
            else -> {
                childBehaviour.handleMessage(message)
                entity.viewEntity.applyMessageToAppearance(message)
            }
        }
    }

    private fun handleApplyBehaviourMessage(message: ApplyBehaviourMessage) {
        childBehaviour = message.createNewBehaviour(childBehaviour)
        childBehaviour.start()
    }

    private fun handleRemoveBehaviourMessage(message: RemoveBehaviourMessage) {
        val child = childBehaviour
        if (child === message.target) {
            childBehaviour = child.childBehaviour
            child.stop()
            return
        }
        childBehaviour.traverseSubtree {
            val itChild = (it as? MetaBehaviour)?.childBehaviour
            if (itChild === message.target) {
                it.childBehaviour = itChild.childBehaviour
                itChild.stop()
            }
        }
    }

}
