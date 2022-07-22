package ru.hse.sd.rgb.gamelogic.engines.experience.scriptactions

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.experience.ExperienceLevelAction
import ru.hse.sd.rgb.gamelogic.engines.items.ItemEntityCreator
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.randomCell
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("spawn_item")
class SpawnItemAction(private val itemEntityCreator: ItemEntityCreator) : ExperienceLevelAction {

    override suspend fun activate(onEntity: GameEntity) {
        val itemEntity = itemEntityCreator.createAt(onEntity.randomCell())
        controller.creation.tryAddToWorld(itemEntity)
    }
}
