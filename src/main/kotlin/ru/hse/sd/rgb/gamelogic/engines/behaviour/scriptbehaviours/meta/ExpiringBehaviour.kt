package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta

import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.MetaBehaviour
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.ExpireTick
import ru.hse.sd.rgb.utils.messaging.messages.RemoveBehaviourMessage

class ExpiringBehaviour(
    entity: GameEntity,
    childBehaviour: Behaviour,
    lastingPeriodMillis: Long,
    temporaryBehaviourFromChild: (Behaviour) -> Behaviour,
) : MetaBehaviour(entity, childBehaviour) {

    private val expireTick = ExpireTick()
    private val expiryTicker = Ticker(lastingPeriodMillis, entity, expireTick)

    private val temporaryBehaviour = temporaryBehaviourFromChild(childBehaviour)

    override fun traverseTickers(onEach: (Ticker) -> Unit) {
        onEach(expiryTicker)
    }

    override suspend fun handleMessage(message: Message) {
        if (message === expireTick) {
            entity.receive(RemoveBehaviourMessage(this))
        } else {
            temporaryBehaviour.handleMessage(message)
        }
    }
}
