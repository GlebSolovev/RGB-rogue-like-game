package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuildingBlock
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.messages.CollidedWith

class ConfuseOnCollision(
    entity: GameEntity,
    childBlock: BehaviourBuildingBlock?,
    private val confuseDurationMillis: Long
) : BehaviourBuildingBlock(entity, childBlock) {
    override suspend fun handleMessage(message: Message) {
        if (message is CollidedWith && message.otherUnit.parentTeamId != entity.fightEntity.teamId) {
            val target = message.otherEntity
            controller.behaviourEngine.applyConfusedBehaviour(target, confuseDurationMillis)
        }
        childBlock?.handleMessage(message)
    }
}
