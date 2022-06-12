package ru.hse.sd.rgb.gamelogic.engines.creation

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.fight.FightEngine
import ru.hse.sd.rgb.gamelogic.engines.physics.PhysicsEngine
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.gameCoroutineScope
import ru.hse.sd.rgb.utils.getValue
import ru.hse.sd.rgb.utils.messaging.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.Dying
import ru.hse.sd.rgb.utils.messaging.messages.LifeEnded
import ru.hse.sd.rgb.utils.messaging.messages.LifeStarted
import ru.hse.sd.rgb.utils.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class CreationEngine(private val physics: PhysicsEngine, private val fightEngine: FightEngine) {

    private val entityCoroutines = ConcurrentHashMap<GameEntity, Job>()
    private var isStopping by AtomicReference(false)
    private val mutex = Mutex()

    suspend fun tryAddToWorld(entity: GameEntity): Boolean {
        mutex.withLock {
            if (isStopping) return false
            return if (tryAddWithoutNotify(entity)) {
                entity.receive(LifeStarted())
                true
            } else false
        }
    }

    private suspend fun tryAddWithoutNotify(entity: GameEntity): Boolean {
        if (!physics.tryPopulate(entity)) return false
        entity.units.forEach { unit -> fightEngine.registerUnit(unit) }
        entityCoroutines[entity] = gameCoroutineScope.launch { entity.messagingRoutine() }
        return true
    }

    suspend fun addAllToWorld(entities: Set<GameEntity>, preStartAction: () -> Unit) {
        if (isStopping) error("cannot add entities using stopped creation engine")
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
        mutex.withLock {
            val experiencePoints = entity.experienceEntity.onDieExperiencePoints
            if (experiencePoints != null) controller.experience.gainExperience(controller.hero, experiencePoints)

            val dieRoutine: suspend () -> Unit = {
                entity.units.forEach { unit -> fightEngine.unregisterUnit(unit) }
                physics.remove(entity)
                entityCoroutines.remove(entity)!!.cancel()
                Ticker.tryStopTickers(entity)
            }
            entity.receive(Dying()) // trigger onDie behaviours
            entity.receive(LifeEnded(dieRoutine)) // finish lifecycle
        }
    }

    suspend fun removeAllAndJoin() {
        mutex.withLock {
            isStopping = true
            entityCoroutines.values.forEach { it.cancelAndJoin() }
//        entityCoroutines.clear() // causes NPE in line 53 (??? after join ???)
        }
    }
}
