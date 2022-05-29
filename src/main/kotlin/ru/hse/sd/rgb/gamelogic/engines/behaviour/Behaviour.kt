package ru.hse.sd.rgb.gamelogic.engines.behaviour

import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.Message

// TODO: generalize duplicated code
// TODO: pass tickers list as arguments with default stop and start implementation
sealed class Behaviour(protected val entity: GameEntity) {

    abstract suspend fun handleMessage(message: Message)

    abstract fun startTickers()

    abstract fun stopTickers()

}
