package ru.hse.sd.rgb.gamelogic.engines.creation

import ru.hse.sd.rgb.gamelogic.engines.fight.FightLogic
import ru.hse.sd.rgb.gamelogic.engines.physics.PhysicsLogic
import ru.hse.sd.rgb.gamelogic.entities.GameEntity

class CreationLogic(private val physics: PhysicsLogic, private val fightLogic: FightLogic) {

    suspend fun tryAddToWorld(entity: GameEntity): Boolean {
        if (!physics.tryPopulate(entity)) return false
        entity.units.forEach { unit -> fightLogic.registerUnit(unit) }
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

