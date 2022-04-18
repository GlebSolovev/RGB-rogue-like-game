package ru.hse.sd.rgb.logic

import ru.hse.sd.rgb.entities.common.GameEntity

class CreationLogic(val physics: PhysicsLogic) {

    suspend fun tryAddToWorld(entity: GameEntity): Boolean {
        return physics.tryPopulate(entity)
    }

}