package ru.hse.sd.rgb.gamelogic.engines.behaviour

import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.messages.SetBehaviour

class BehaviourEngine {

    fun setPassiveBehaviourFor(entity: GameEntity) {
        entity.receive(SetBehaviour { entity.behaviourEntity.createPassiveBehaviour() })
    }

}