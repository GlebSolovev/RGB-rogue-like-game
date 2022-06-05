package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuildingBlock
import ru.hse.sd.rgb.gamelogic.engines.fight.ControlParams
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.messages.ColorTick

class EnableColorUpdate(
    entity: GameEntity,
    childBlock: BehaviourBuildingBlock?,
    private val controlParams: ControlParams
) : BehaviourBuildingBlock(entity, childBlock) {

    override suspend fun handleMessage(message: Message) {
        if (message is ColorTick) controller.fighting.update(message.unit, controlParams)
        else childBlock?.handleMessage(message)
    }
}
