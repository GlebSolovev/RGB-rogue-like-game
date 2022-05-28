package ru.hse.sd.rgb.gamelogic.behaviours

import ru.hse.sd.rgb.utils.messaging.Message

abstract class SimpleBehaviour : Behaviour {

    protected abstract var state: State

    override suspend fun handleMessage(message: Message) {
        state = state.next(message)
    }

}
