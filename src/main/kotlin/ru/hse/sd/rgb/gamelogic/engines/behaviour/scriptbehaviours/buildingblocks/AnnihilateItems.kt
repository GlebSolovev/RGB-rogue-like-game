package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuildingBlock
import ru.hse.sd.rgb.gamelogic.engines.items.ItemEntity
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.messages.CollidedWith

class AnnihilateItems(entity: GameEntity, childBlock: BehaviourBuildingBlock?) :
    BehaviourBuildingBlock(entity, childBlock) {

    override suspend fun handleMessage(message: Message) {
        if (message is CollidedWith) {
            val other = message.otherEntity
            if (other is ItemEntity) {
                controller.itemsEngine.tryPick(entity, other)
            }
        }
        childBlock?.handleMessage(message)
    }
}
