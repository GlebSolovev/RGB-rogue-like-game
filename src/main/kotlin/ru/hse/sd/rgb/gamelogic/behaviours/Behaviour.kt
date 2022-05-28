package ru.hse.sd.rgb.gamelogic.behaviours

import ru.hse.sd.rgb.utils.messaging.Message

sealed interface Behaviour {

    suspend fun handleMessage(message: Message)

}