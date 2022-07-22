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
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * Class for unified and consistent [GameEntity] creation and removal.
 *
 * By 'creation' we mean adding the entity to the game world:
 *  - adding the entity to all necessary engines
 *  - sending all necessary messages
 *  - starting entity's messaging coroutine
 *
 *  @param physics The [PhysicsEngine] that will be used with this CreationEngine.
 *  @param fightEngine The [FightEngine] that will be used with this CreationEngine.
 *  @constructor Creates a CreationEngine with no entities.
 */
class CreationEngine(private val physics: PhysicsEngine, private val fightEngine: FightEngine) {

    private val entityCoroutines = ConcurrentHashMap<GameEntity, Job>()
    private var isStopping by AtomicReference(false)
    private val mutex = Mutex()

    /**
     * Tries to add [entity] to the game world.
     *
     * If any of the engines refuse to add [entity], it is guaranteed that [entity] is
     * added to none of them.
     *
     * If [entity] was successfully added to all engines, it receives a [LifeStarted]
     * message and its messaging coroutine is started.
     *
     * @return true if [entity] was successfully added to game world, false otherwise.
     */
    suspend fun tryAddToWorld(entity: GameEntity): Boolean {
        return if (tryAddWithoutNotify(entity)) {
            entity.receive(LifeStarted())
            true
        } else false
    }

    private suspend fun tryAddWithoutNotify(entity: GameEntity): Boolean {
        mutex.withLock {
            if (isStopping) return false

            if (!physics.tryPopulate(entity)) return false
            entity.units.forEach { unit -> fightEngine.registerUnit(unit) }
            entityCoroutines[entity] =
                gameCoroutineScope.launch(CoroutineName("coro $entity")) { entity.messagingRoutine() }
            return true
        }
    }

    /**
     * Adds several [entities] to game world at once.
     *
     * Unlike [tryAddToWorld], if some of the [entities] can't be added, this method throws
     * an [IllegalStateException].
     *
     * After all [entities] were successfully added to all engines, all [entities] receive a
     * [LifeStarted] message and their coroutines are started.
     *
     * This method also allows to execute some action after all [entities] have been added to
     * engines, but before any of them received a [LifeStarted] message.
     *
     * @param entities The entities to add to game world.
     * @param preStartAction The action to perform in between adding entities to engines and
     * sending them a [LifeStarted] message.
     */
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

    /**
     * Removes [entity] from game world. This method is basically an inverse of [tryAddToWorld] method.
     *
     * To ensure thread-safety, this method does not perform almost any action at call-time. Instead,
     * it creates a special self-destruct [LifeEnded] message and sends it to [entity], and therefore
     * [entity] only gets removed when it processes that message itself. Consequently, [entity]
     * might still exist in game world for some time even after this method returns.
     *
     * Before receiving the [LifeEnded], [entity] will also receive [Dying] message.
     *
     * Also, the hero will receive the experience from this entity as defined by
     * [GameEntity.ExperienceEntity].
     *
     * @param entity The entity to be removed from game world.
     */
    suspend fun die(entity: GameEntity) {
        // TODO: hero shouldn't receive experience for entities that died on their own
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

    /**
     * Removes all alive entities through stopping their coroutines. Waits for stop of each coroutine.
     */
    suspend fun removeAllAndJoin() {
        mutex.withLock {
            isStopping = true
            entityCoroutines.values.forEach { it.cancelAndJoin() }
//        entityCoroutines.clear() // causes NPE in line 53 (??? after join ???)
        }
    }
}
