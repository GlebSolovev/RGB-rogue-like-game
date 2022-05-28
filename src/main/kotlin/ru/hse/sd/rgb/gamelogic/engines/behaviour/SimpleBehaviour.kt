package ru.hse.sd.rgb.gamelogic.engines.behaviour

import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.Message

abstract class SimpleBehaviour(entity: GameEntity) : Behaviour(entity) {

    protected abstract var state: State

    override suspend fun handleMessage(message: Message) {
        state = state.next(message)
    }

}
