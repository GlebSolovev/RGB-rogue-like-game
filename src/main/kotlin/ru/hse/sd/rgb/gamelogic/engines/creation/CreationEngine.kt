package ru.hse.sd.rgb.gamelogic.engines.creation

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.hse.sd.rgb.gamelogic.engines.fight.FightEngine
import ru.hse.sd.rgb.gamelogic.engines.physics.PhysicsEngine
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.LifeEnded
import ru.hse.sd.rgb.gamelogic.entities.LifeStarted
import ru.hse.sd.rgb.gamelogic.gameCoroutineScope
import ru.hse.sd.rgb.utils.Ticker
import java.util.concurrent.ConcurrentHashMap

class CreationEngine(private val physics: PhysicsEngine, private val fightEngine: FightEngine) {

    private val entityCoroutines = ConcurrentHashMap<GameEntity, Job>()

    suspend fun tryAddToWorld(entity: GameEntity): Boolean {
        if (tryAddWithoutNotify(entity)) {
            entity.receive(LifeStarted())
            return true
        }
        return false
    }

    private suspend fun tryAddWithoutNotify(entity: GameEntity): Boolean {
        if (!physics.tryPopulate(entity)) return false
        entity.units.forEach { unit -> fightEngine.registerUnit(unit) }
        entityCoroutines[entity] = gameCoroutineScope.launch { entity.messagingRoutine() }
        return true
    }

    suspend fun addAllToWorld(entities: Set<GameEntity>, preStartAction: () -> Unit) {
        for (entity in entities) {
            if (!tryAddWithoutNotify(entity)) throw IllegalStateException("invalid entities")
        }
        preStartAction()
        for (entity in entities) {
            entity.receive(LifeStarted())
        }
    }

    // entity must not react on new events after calling with method
    suspend fun die(entity: GameEntity) {
        val dieRoutine: suspend () -> Unit = {
            entity.units.forEach { unit -> fightEngine.unregisterUnit(unit) }
            physics.remove(entity)
            entityCoroutines.remove(entity)!!.cancel()
            Ticker.stopTickers(entity)
        }
        entity.receive(LifeEnded(dieRoutine))
    }

}

