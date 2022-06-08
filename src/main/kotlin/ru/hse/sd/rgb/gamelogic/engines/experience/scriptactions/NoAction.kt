package ru.hse.sd.rgb.gamelogic.engines.experience.scriptactions

import ru.hse.sd.rgb.gamelogic.engines.experience.ExperienceLevelAction
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.ignore
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("no_action")
class NoAction : ExperienceLevelAction {
    override suspend fun activate(onEntity: GameEntity) = ignore
}
