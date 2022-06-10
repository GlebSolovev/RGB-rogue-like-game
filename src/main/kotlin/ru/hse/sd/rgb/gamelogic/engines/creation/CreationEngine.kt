package ru.hse.sd.rgb.gamelogic.engines.creation

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.ciPrint
import ru.hse.sd.rgb.gamelogic.engines.fight.FightEngine
import ru.hse.sd.rgb.gamelogic.engines.physics.PhysicsEngine
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.gameCoroutineScope
import ru.hse.sd.rgb.utils.messaging.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.Dying
import ru.hse.sd.rgb.utils.messaging.messages.LifeEnded
import ru.hse.sd.rgb.utils.messaging.messages.LifeStarted
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class CreationEngine(private val physics: PhysicsEngine, private val fightEngine: FightEngine) {

    private val entityCoroutines = ConcurrentHashMap<GameEntity, Job>()

    suspend fun tryAddToWorld(entity: GameEntity): Boolean {
        return if (tryAddWithoutNotify(entity)) {
            entity.receive(LifeStarted())
            true
        } else false
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
        val experiencePoints = entity.experienceEntity.onDieExperiencePoints
        if (experiencePoints != null) controller.experience.gainExperience(controller.hero, experiencePoints)

        val dieRoutine: suspend () -> Unit = {
            ciPrint("$entity is dying")
            entity.units.forEach { unit -> fightEngine.unregisterUnit(unit) }
            ciPrint("$entity is dying: fighting")
            physics.remove(entity)
            ciPrint("$entity is dying: physics")
            entityCoroutines.remove(entity)!!.cancel()
            ciPrint("$entity is dying: job")
            Ticker.tryStopTickers(entity)
        }
        entity.receive(Dying()) // trigger onDie behaviours
        entity.receive(LifeEnded(dieRoutine)) // finish lifecycle
    }

    suspend fun removeAllAndJoin() {
        entityCoroutines.values.forEach { it.cancelAndJoin() }
        ciPrint("creation joined all")
//        entityCoroutines.clear() // causes NPE in line 53 (??? after join ???)
    }
}
