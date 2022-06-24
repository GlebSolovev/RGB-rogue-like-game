package ru.hse.sd.rgb.gamelogic.engines.experience

import ru.hse.sd.rgb.gamelogic.engines.experience.scriptactions.NoAction
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Hero
import ru.hse.sd.rgb.utils.messaging.messages.ExperienceLevelUpdate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

@Serializable
data class ExperienceLevelDescription(
    val requiredPoints: Int, // each new level current experience points resets to 0
    val actions: List<ExperienceLevelAction>,
)

data class Experience(val points: Int, val level: Int)
data class DetailedExperience(val points: Int, val level: Int, val nextLevelRequiredPoints: Int?)

// so far only Hero experienceLevels are supported, basically extensible to any GameEntity classes
class ExperienceEngine(heroExperienceLevels: List<ExperienceLevelDescription>) {

    private val experienceLevels = ConcurrentHashMap<KClass<out GameEntity>, List<ExperienceLevelDescription>>()

    init {
        validateExperienceLevels(heroExperienceLevels)

        val levelDescription = ExperienceLevelDescription(0, listOf(NoAction()))
        experienceLevels[Hero::class] = listOf(levelDescription) + heroExperienceLevels
    }

    private fun validateExperienceLevels(experienceLevels: List<ExperienceLevelDescription>) {
        for (level in experienceLevels) {
            if (level.requiredPoints <= 0)
                throw IllegalArgumentException("levels' required points must be positive")
        }
    }

    private val entityMutexes = ConcurrentHashMap<GameEntity, Mutex>()
    private val currentExperience = ConcurrentHashMap<GameEntity, CurrentExperience>()
    private val subscriptionsToLevelUpdateOf = ConcurrentHashMap<GameEntity, MutableSet<GameEntity>>()

    suspend fun registerEntity(entity: GameEntity, experience: Experience = Experience(0, 0)) {
        withLockedEntity(entity) {
            if (!experienceLevels.containsKey(entity::class))
                throw IllegalArgumentException("no experience levels script for ${entity::class}")
            if (experience.points < 0 || experience.level !in 0 until experienceLevels[entity::class]!!.size)
                throw IllegalArgumentException("illegal experience")

            currentExperience[entity] = CurrentExperience(experience)
            val subscribers = subscriptionsToLevelUpdateOf.getOrPut(entity) { mutableSetOf() }

            for (subscriber in subscribers) {
                subscriber.receive(ExperienceLevelUpdate(entity, experience.level))
            }
        }
    }

    suspend fun unregisterEntity(entity: GameEntity) {
        withLockedEntity(entity) {
            entityMutexes.remove(entity)
            currentExperience.remove(entity)
            subscriptionsToLevelUpdateOf.remove(entity)
        }
    }

    suspend fun gainExperience(entity: GameEntity, points: Int) {
        withLockedEntity(entity) {
            currentExperience[entity]!!.points += points

            val entityExperienceLevels = experienceLevels[entity::class]!!
            val entityCurrentExperience = currentExperience[entity]!!

            while (entityCurrentExperience.level < entityExperienceLevels.size - 1 &&
                entityExperienceLevels[entityCurrentExperience.level + 1]
                    .requiredPoints <= entityCurrentExperience.points
            ) {
                entityCurrentExperience.level += 1
                val newLevelDescription = entityExperienceLevels[entityCurrentExperience.level]

                entityCurrentExperience.points -= newLevelDescription.requiredPoints
                newLevelDescription.actions.forEach { it.activate(entity) }

                for (subscriber in subscriptionsToLevelUpdateOf[entity]!!) {
                    subscriber.receive(ExperienceLevelUpdate(entity, entityCurrentExperience.level))
                }
            }
        }
    }

    suspend fun getExperience(entity: GameEntity): Experience? {
        return withLockedEntity(entity) {
            val (points, level) = currentExperience[entity]!!
            Experience(points, level)
        }
    }

    suspend fun getDetailedExperience(entity: GameEntity): DetailedExperience? {
        return withLockedEntity(entity) {
            val (points, level) = currentExperience[entity]!!
            val entityExperienceLevels = experienceLevels[entity::class]!!
            DetailedExperience(
                points,
                level,
                if (level == entityExperienceLevels.size - 1) null else entityExperienceLevels[level + 1].requiredPoints
            )
        }
    }

    suspend fun subscribeToExperienceLevelUpdate(subscriber: GameEntity, toEntity: GameEntity) {
        withLockedEntity(toEntity) {
            val subscribers = subscriptionsToLevelUpdateOf.getOrPut(toEntity) { mutableSetOf() }
            subscribers.add(subscriber)

            val toEntityCurrentLevel = currentExperience[toEntity]?.level ?: return@withLockedEntity
            subscriber.receive(ExperienceLevelUpdate(toEntity, toEntityCurrentLevel))
        }
    }

    suspend fun unsubscribeFromExperienceLevelUpdate(subscriber: GameEntity, toEntity: GameEntity) {
        withLockedEntity(toEntity) {
            subscriptionsToLevelUpdateOf[toEntity]!!.remove(subscriber)
                .let { if (!it) throw IllegalArgumentException("forbid to unsubscribe not subscribed entity") }
        }
    }

    private suspend inline fun <R> withLockedEntity(entity: GameEntity, crossinline block: suspend () -> R): R? {
        val mutex = entityMutexes.getOrPut(entity) { Mutex() }
        mutex.withLock {
            if (!entityMutexes.containsKey(entity)) return null
            return block()
        }
    }

    private data class CurrentExperience(var points: Int, var level: Int) {
        constructor(experience: Experience) : this(experience.points, experience.level)
    }
}
