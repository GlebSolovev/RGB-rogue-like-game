package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuildingBlock
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.messages.HpChanged

class DieOnFatalAttack(entity: GameEntity, childBlock: BehaviourBuildingBlock?) :
    BehaviourBuildingBlock(entity, childBlock) {

    override suspend fun handleMessage(message: Message) {
        if (message is HpChanged && message.isFatal) controller.creation.die(entity)
        childBlock?.handleMessage(message)
    }
}
