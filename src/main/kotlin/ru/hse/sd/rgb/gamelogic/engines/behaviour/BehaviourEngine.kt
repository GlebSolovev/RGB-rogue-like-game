package ru.hse.sd.rgb.gamelogic.engines.behaviour

import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.messages.SetBehaviour

class BehaviourEngine {

    fun setPassiveBehaviour(entity: GameEntity, movePeriodMillis: Long) {
        entity.receive(SetBehaviour { entity.behaviourEntity.createPassiveBehaviour(movePeriodMillis) })
    }

    fun setAttackUponSeeingMetaBehaviour(
        entity: GameEntity,
        initialBehaviour: Behaviour,
        targetEntity: GameEntity,
        seeingDepth: Int,
        directAttackMovePeriodMillis: Long,
        watchPeriodMillis: Long
    ) {
        entity.receive(SetBehaviour {
            entity.behaviourEntity.createAttackUponSeeingMetaBehaviour(
                initialBehaviour,
                targetEntity,
                seeingDepth,
                directAttackMovePeriodMillis,
                watchPeriodMillis
            )
        })
    }

    fun setFleeUponSeeingMetaBehaviour(
        entity: GameEntity,
        initialBehaviour: Behaviour,
        targetEntity: GameEntity,
        seeingDepth: Int,
        directFleeMovePeriodMillis: Long,
        watchPeriodMillis: Long
    ) {
        entity.receive(SetBehaviour {
            entity.behaviourEntity.createFleeUponSeeingMetaBehaviour(
                initialBehaviour,
                targetEntity,
                seeingDepth,
                directFleeMovePeriodMillis,
                watchPeriodMillis
            )
        })
    }

}