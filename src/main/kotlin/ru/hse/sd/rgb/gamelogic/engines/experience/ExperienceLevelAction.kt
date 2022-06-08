package ru.hse.sd.rgb.gamelogic.engines.experience

import ru.hse.sd.rgb.gamelogic.entities.GameEntity

// is called under locked mutex for onEntity, beware of deadlocks
// NOTE: so far ExperienceLevelAction is expected not to interact directly with experience of onEntity
interface ExperienceLevelAction {
    suspend fun activate(onEntity: GameEntity)
}
