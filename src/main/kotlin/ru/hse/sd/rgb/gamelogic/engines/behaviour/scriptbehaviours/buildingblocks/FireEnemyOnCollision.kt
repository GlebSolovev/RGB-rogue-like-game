package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuildingBlock
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.messages.CollidedWith

class FireEnemyOnCollision(
    entity: GameEntity,
    childBlock: BehaviourBuildingBlock?,
    private val burningAttackPeriodMillis: Long,
    private val burningAttack: Int,
    private val burningDurationMillis: Long?
) :
    BehaviourBuildingBlock(entity, childBlock) {

    override suspend fun handleMessage(message: Message) {
        if (message is CollidedWith) {
            val other = message.otherEntity
            if (entity.fightEntity.teamId != other.fightEntity.teamId) {
                controller.behaviourEngine.applyBurningBehaviour(
                    other,
                    burningAttackPeriodMillis,
                    burningAttack,
                    burningDurationMillis
                )
            }
        }
        childBlock?.handleMessage(message)
    }
}
