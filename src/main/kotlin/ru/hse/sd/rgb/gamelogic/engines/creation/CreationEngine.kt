package ru.hse.sd.rgb.gamelogic.engines.creation

import kotlinx.coroutines.launch
import ru.hse.sd.rgb.gamelogic.engines.fight.FightEngine
import ru.hse.sd.rgb.gamelogic.engines.physics.PhysicsEngine
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameStarted
import ru.hse.sd.rgb.gamelogic.gameCoroutineScope

class CreationEngine(private val physics: PhysicsEngine, private val fightEngine: FightEngine) {

    suspend fun tryAddToWorld(entity: GameEntity): Boolean {
        if (tryAddWithoutNotify(entity)) {
            entity.receive(GameStarted())
            return true
        }
        return false
    }

    private suspend fun tryAddWithoutNotify(entity: GameEntity): Boolean {
        if (!physics.tryPopulate(entity)) return false
        entity.units.forEach { unit -> fightEngine.registerUnit(unit) }
        gameCoroutineScope.launch { entity.messagingRoutine() }
        return true
    }

    suspend fun addAllToWorld(entities: Set<GameEntity>, preStartAction: () -> Unit) {
        for (entity in entities) {
            if (!tryAddWithoutNotify(entity)) throw IllegalStateException("invalid entities")
        }
        preStartAction()
        for (entity in entities) {
            entity.receive(GameStarted())
        }
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
//        TODO()
    }

}

