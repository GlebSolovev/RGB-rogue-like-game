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
        durationPeriodMillis: Long?
    ) {
        if (durationPeriodMillis == null) {
            entity.receive(
                ApplyBehaviourMessage {
                    entity.behaviourEntity.createConfusedBehaviour(it)
                }
            )
        } else {
            applyExpiringBehaviour(entity, durationPeriodMillis) {
                entity.behaviourEntity.createConfusedBehaviour(it)
            }
        }
    }

    private fun applyExpiringBehaviour(
        entity: GameEntity,
        durationPeriodMillis: Long,
        createTemporaryBehaviour: (Behaviour) -> Behaviour,
    ) {
        entity.receive(
            ApplyBehaviourMessage {
                ExpiringBehaviour(entity, it, durationPeriodMillis, createTemporaryBehaviour)
            }
        )
    }
}
