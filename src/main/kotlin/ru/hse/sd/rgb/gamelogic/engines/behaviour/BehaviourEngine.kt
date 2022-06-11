package ru.hse.sd.rgb.gamelogic.engines.behaviour

import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta.ExpiringBehaviour
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.messages.ApplyBehaviourMessage
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta.*

/**
 * Class for applying [MetaBehaviour]s to [GameEntity]-s.
 *
 * This class is basically a facade for several boilerplate methods. The key idea is that
 * applying a behaviour to an entity requires accessing its [GameEntity.BehaviourEntity],
 * which at call-site is too verbose and error-prone.
 *
 * Another feature this class provides is implicitly wrapping some behaviours into an
 * [ExpiringBehaviour], which allows applying temporary behaviours.
 */
class BehaviourEngine {

    /**
     * Applies [DirectAttackHeroBehaviour] to [entity].
     *
     * @param entity The entity to apply the behaviour to.
     * @param movePeriodMillis [DirectAttackHeroBehaviour] parameter.
     */
    fun applyDirectAttackHeroBehaviour(
        entity: GameEntity,
        movePeriodMillis: Long,
    ) {
        entity.receive(
            ApplyBehaviourMessage {
                entity.behaviourEntity.createDirectAttackHeroBehaviour(it, movePeriodMillis)
            }
        )
    }

    /**
     * Applies [DirectFleeFromHeroBehaviour] to [entity].
     *
     * @param entity The entity to apply the behaviour to.
     * @param movePeriodMillis [DirectFleeFromHeroBehaviour] parameter.
     */
    fun applyDirectFleeFromHeroBehaviour(
        entity: GameEntity,
        movePeriodMillis: Long,
    ) {
        entity.receive(
            ApplyBehaviourMessage {
                entity.behaviourEntity.createDirectFleeFromHeroBehaviour(it, movePeriodMillis)
            }
        )
    }

    /**
     * Applies [UponSeeingBehaviour] to [entity].
     *
     * @param entity The entity to apply the behaviour to.
     * @param targetEntity [UponSeeingBehaviour] parameter.
     * @param seeingDepth [UponSeeingBehaviour] parameter.
     * @param createSeeingBehaviour [UponSeeingBehaviour] parameter.
     */
    fun applyUponSeeingBehaviour(
        entity: GameEntity,
        targetEntity: GameEntity,
        seeingDepth: Int,
        createSeeingBehaviour: (Behaviour) -> Behaviour
    ) {
        entity.receive(
            ApplyBehaviourMessage {
                entity.behaviourEntity.createUponSeeingBehaviour(
                    it,
                    targetEntity,
                    seeingDepth,
                    createSeeingBehaviour
                )
            }
        )
    }

    /**
     * Applies expiring [ConfusedBehaviour] to [entity].
     *
     * @param entity The entity to apply the behaviour to.
     * @param durationMillis The duration for which [ConfusedBehaviour] should last.
     * If null, the new behaviour is permanent.
     */
    fun applyConfusedBehaviour(entity: GameEntity, durationMillis: Long?) {
        wrapIntoExpiringIfDurationIsNotNull(entity, durationMillis) {
            entity.behaviourEntity.createConfusedBehaviour(it)
        }
    }

    /**
     * Applies expiring [BurningBehaviour] to [entity].
     *
     * @param entity The entity to apply the behaviour to.
     * @param attackPeriodMillis [BurningBehaviour] parameter.
     * @param attack [BurningBehaviour] parameter.
     * @param durationMillis The duration for which [BurningBehaviour] should last.
     * If null, the new behaviour is permanent.
     */
    fun applyBurningBehaviour(
        entity: GameEntity,
        attackPeriodMillis: Long,
        attack: Int,
        durationMillis: Long?
    ) {
        wrapIntoExpiringIfDurationIsNotNull(entity, durationMillis) {
            entity.behaviourEntity.createBurningBehaviour(it, attackPeriodMillis, attack, durationMillis)
        }
    }

    /**
     * Applies expiring [FrozenBehaviour] to [entity].
     *
     * @param entity The entity to apply the behaviour to.
     * @param slowDownCoefficient [FrozenBehaviour] parameter.
     * @param durationMillis The duration for which [FrozenBehaviour] should last.
     * If null, the new behaviour is permanent.
     */
    fun applyFrozenBehaviour(entity: GameEntity, slowDownCoefficient: Double, durationMillis: Long?) {
        wrapIntoExpiringIfDurationIsNotNull(entity, durationMillis) {
            entity.behaviourEntity.createFrozenBehaviour(it, slowDownCoefficient)
        }
    }

    private fun wrapIntoExpiringIfDurationIsNotNull(
        entity: GameEntity,
        durationMillis: Long?,
        createBehaviour: (Behaviour) -> Behaviour
    ) {
        if (durationMillis == null) {
            entity.receive(ApplyBehaviourMessage { createBehaviour(it) })
        } else {
            applyExpiringBehaviour(entity, durationMillis) { createBehaviour(it) }
        }
    }

    private fun applyExpiringBehaviour(
        entity: GameEntity,
        durationMillis: Long,
        createTemporaryBehaviour: (Behaviour) -> Behaviour,
    ) {
        entity.receive(
            ApplyBehaviourMessage {
                ExpiringBehaviour(entity, it, durationMillis, createTemporaryBehaviour)
            }
        )
    }
}
