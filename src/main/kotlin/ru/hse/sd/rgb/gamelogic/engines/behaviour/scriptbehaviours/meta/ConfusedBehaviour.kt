package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuilder
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuildingBlock
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.Direction
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.messages.EntityUpdated
import ru.hse.sd.rgb.utils.messaging.messages.MoveTick

fun ConfusedBehaviour(entity: GameEntity, childBehaviour: Behaviour) = BehaviourBuilder.metaFromBlocks(childBehaviour)
    .add {
        object : BehaviourBuildingBlock(entity, childBlock) {
            override suspend fun handleMessage(message: Message) {
                if (message is MoveTick) {
                    val dir = Direction.random()
                    val moved = controller.physics.tryMove(entity, dir)
                    if (moved) controller.view.receive(EntityUpdated(entity))
                } else {
                    childBlock?.handleMessage(message)
                }
            }
        }
    }
    .build()
