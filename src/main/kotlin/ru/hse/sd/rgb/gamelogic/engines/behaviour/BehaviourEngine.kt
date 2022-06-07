package ru.hse.sd.rgb.gamelogic.engines.behaviour

import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta.ExpiringBehaviour
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.messages.ApplyBehaviourMessage

class BehaviourEngine {

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

    fun applyConfusedBehaviour(
        entity: GameEntity,
        durationMillis: Long?
    ) {
        if (durationMillis == null) {
            entity.receive(
                ApplyBehaviourMessage {
                    entity.behaviourEntity.createConfusedBehaviour(it)
                }
            )
        } else {
            applyExpiringBehaviour(entity, durationMillis) {
                entity.behaviourEntity.createConfusedBehaviour(it)
            }
        }
    }

    fun applyBurningBehaviour(
        entity: GameEntity,
        attackPeriodMillis: Long,
        attack: Int,
        durationMillis: Long?
    ) {
        if (durationMillis == null) {
            entity.receive(
                ApplyBehaviourMessage {
                    entity.behaviourEntity.createBurningBehaviour(it, attackPeriodMillis, attack, durationMillis)
                }
            )
        } else {
            applyExpiringBehaviour(entity, durationMillis) {
                entity.behaviourEntity.createBurningBehaviour(it, attackPeriodMillis, attack, durationMillis)
            }
        }
    }
    // TODO: wrap durationMillis == null case

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
