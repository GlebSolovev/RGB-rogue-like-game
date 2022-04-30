package ru.hse.sd.rgb.gamelogic.engines.creation

import ru.hse.sd.rgb.gamelogic.engines.fight.FightEngine
import ru.hse.sd.rgb.gamelogic.engines.physics.PhysicsEngine
import ru.hse.sd.rgb.gamelogic.entities.GameEntity

class CreationEngine(private val physics: PhysicsEngine, private val fightEngine: FightEngine) {

    suspend fun tryAddToWorld(entity: GameEntity): Boolean {
        if (!physics.tryPopulate(entity)) return false
        entity.units.forEach { unit -> fightEngine.registerUnit(unit) }
        return true
    }

//    // must be called first before removing unit from units set of Entity
//    suspend fun deleteUnitFromWorld(unit: GameUnit) {
//        fightLogic.unregisterUnit(unit)
//
//    }
//
//    suspend fun deleteFromWorld(entity: GameEntity) {
//        entity.units.forEach { unit -> fightLogic.unregisterUnit(unit) }
//    }

    fun tryDie(entity: GameEntity) {
        TODO()
    }

}

