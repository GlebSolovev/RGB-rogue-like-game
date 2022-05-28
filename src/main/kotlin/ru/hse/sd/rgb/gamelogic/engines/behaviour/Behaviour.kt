package ru.hse.sd.rgb.gamelogic.engines.behaviour

import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.Message

sealed class Behaviour(protected val entity: GameEntity) {

    abstract suspend fun handleMessage(message: Message)

    abstract fun stopTickers()

}
