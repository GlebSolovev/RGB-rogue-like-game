package ru.hse.sd.rgb.gamelogic.engines.behaviour

import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.messages.ApplyBehaviourMessage

class BehaviourEngine {

    fun applyDirectAttackHeroBehaviour(
        entity: GameEntity,
        movePeriodMillis: Long,
    ) {
        entity.receive(ApplyBehaviourMessage {
            entity.behaviourEntity.createDirectAttackHeroBehaviour(it, movePeriodMillis)
        })
    }

    fun applyDirectFleeFromHeroBehaviour(
        entity: GameEntity,
        movePeriodMillis: Long,
    ) {
        entity.receive(ApplyBehaviourMessage {
            entity.behaviourEntity.createDirectFleeFromHeroBehaviour(it, movePeriodMillis)
        })
    }

    fun applyUponSeeingBehaviour(
        entity: GameEntity,
        targetEntity: GameEntity,
        seeingDepth: Int,
        createSeeingBehaviour: (Behaviour) -> Behaviour
    ) {
        entity.receive(ApplyBehaviourMessage {
            entity.behaviourEntity.createUponSeeingBehaviour(
                it,
                targetEntity,
                seeingDepth,
                createSeeingBehaviour
            )
        })
    }

    // note: is permanent
    fun applyConfusedBehaviour(
        entity: GameEntity,
    ) {
        entity.receive(ApplyBehaviourMessage {
            entity.behaviourEntity.createConfusedBehaviour(it)
        })
    }

    // TODO: currently caller must use entity.behaviourEntity to create appropriate temporary behaviour
    // but the whole purpose of this class is to avoid that

    fun applyExpiringBehaviour(
        entity: GameEntity,
        durationPeriodMillis: Long,
        createTemporaryBehaviour: (Behaviour) -> Behaviour,
    ) {
        entity.receive(ApplyBehaviourMessage {
            entity.behaviourEntity.createExpiringBehaviour(
                it,
                durationPeriodMillis,
                createTemporaryBehaviour
            )
        })
    }
}
