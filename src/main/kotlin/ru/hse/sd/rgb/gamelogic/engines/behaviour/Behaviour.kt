package ru.hse.sd.rgb.gamelogic.engines.behaviour

import ru.hse.sd.rgb.utils.messaging.Message

sealed interface Behaviour {

    suspend fun handleMessage(message: Message)

}