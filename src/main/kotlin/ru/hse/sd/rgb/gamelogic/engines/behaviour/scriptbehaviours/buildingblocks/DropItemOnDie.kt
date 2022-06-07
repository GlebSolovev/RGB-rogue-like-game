package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuildingBlock
import ru.hse.sd.rgb.gamelogic.engines.items.ItemEntity
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.messages.Dying
import ru.hse.sd.rgb.utils.nextChance
import kotlin.random.Random

class DropItemOnDie(
    entity: GameEntity,
    childBlock: BehaviourBuildingBlock?,
    private val probability: Double,
    private val random: Random = Random,
    private val createItemEntity: (GameEntity) -> ItemEntity
) :
    BehaviourBuildingBlock(entity, childBlock) {

    init {
        if (probability !in 0.0..1.0) throw IllegalArgumentException("probability must be in 0..1")
    }

    override suspend fun handleMessage(message: Message) {
        if (message is Dying && random.nextChance(probability)) {
            controller.creation.tryAddToWorld(createItemEntity(entity))
        }
        childBlock?.handleMessage(message)
    }
}
