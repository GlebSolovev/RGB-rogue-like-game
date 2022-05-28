package ru.hse.sd.rgb.gamelogic.engines.behaviour

import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.Message

abstract class MetaBehaviour(protected val initialBehaviour: Behaviour, entity: GameEntity) : Behaviour(entity) {

    protected abstract var metaState: State

    override suspend fun handleMessage(message: Message) {
        metaState = metaState.next(message)
    }

}
