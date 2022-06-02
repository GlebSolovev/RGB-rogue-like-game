package ru.hse.sd.rgb.gamelogic.engines.behaviour

import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.ignore
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.Tick
import ru.hse.sd.rgb.utils.messaging.Ticker
import kotlin.reflect.KClass

open class NoneBehaviour(entity: GameEntity) : Behaviour(entity) {

    override suspend fun handleMessage(message: Message) = ignore

    override fun traverseTickers(onEach: (Ticker) -> Unit) = ignore

    final override fun traverseSubtree(onEach: (Behaviour) -> Unit) = onEach(this)

    private val tickers = mutableMapOf<KClass<out Tick>, MutableSet<Ticker>>()

    override fun tickersGroup(tickClass: KClass<out Tick>) = tickers.getOrPut(tickClass) { mutableSetOf() }
}
