package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta

import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.MetaBehaviour
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.OneTimeTicker
import ru.hse.sd.rgb.utils.messaging.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.ExpireTick
import ru.hse.sd.rgb.utils.messaging.messages.RemoveBehaviourMessage

class ExpiringBehaviour(
    entity: GameEntity,
    childBehaviour: Behaviour,
    lastingPeriodMillis: Long,
    createTemporaryBehaviour: (Behaviour) -> Behaviour,
) : MetaBehaviour(entity, childBehaviour) {

    private val expireTick = ExpireTick() // NOTE: for === below
    private val expiryTicker = OneTimeTicker(lastingPeriodMillis, entity, expireTick)

    private val temporaryBehaviour = createTemporaryBehaviour(childBehaviour)

    override fun traverseTickers(onEach: (Ticker) -> Unit) {
        onEach(expiryTicker)
    }

    override fun traverseSubtree(onEach: (Behaviour) -> Unit) {
        onEach(this)
        temporaryBehaviour.traverseSubtree(onEach)
    }

    override fun onStart() {
        temporaryBehaviour.onStart()
    }

    override fun onStop() {
        temporaryBehaviour.onStop()
    }

    override suspend fun handleMessage(message: Message) {
        if (message === expireTick) {
            entity.receive(RemoveBehaviourMessage(this))
        } else {
            temporaryBehaviour.handleMessage(message)
        }
    }
}
