package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.simple

import ru.hse.sd.rgb.gamelogic.engines.behaviour.SimpleBehaviour
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.MoveTick

abstract class MoveSimpleBehaviour(entity: GameEntity, movePeriodMillis: Long) : SimpleBehaviour(entity) {

    private val moveTicker = Ticker(movePeriodMillis, entity, MoveTick()).also { it.start() }

    override fun stopTickers() {
        moveTicker.stop()
    }
}
