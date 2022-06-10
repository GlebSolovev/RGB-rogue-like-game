package ru.hse.sd.rgb.gamelogic.engines.behaviour

import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.Tick
import ru.hse.sd.rgb.utils.messaging.Ticker
import kotlin.reflect.KClass

abstract class BehaviourBuildingBlock(
    protected val entity: GameEntity,
    val childBlock: BehaviourBuildingBlock?,
) {
    abstract suspend fun handleMessage(message: Message)
    open val ticker: Ticker? = null
}

sealed class Behaviour(val entity: GameEntity) {
    abstract suspend fun handleMessage(message: Message)
    abstract fun tickersGroup(tickClass: KClass<out Tick>): MutableSet<Ticker>

    abstract fun traverseTickers(onEach: (Ticker) -> Unit)
    open fun onStart() {}
    open fun onStop() {}

    fun start() {
        traverseTickers { ticker ->
            ticker.start()
            tickersGroup(ticker.tick::class).add(ticker)
        }
        onStart()
    }

    fun stop() {
        traverseTickers { ticker ->
            ticker.stop()
            tickersGroup(ticker.tick::class).remove(ticker)
        }
        onStop()
    }

    abstract fun traverseSubtreeForTickers(onEach: (Behaviour) -> Unit)

    fun startSubtree() = traverseSubtreeForTickers { it.start() }

    fun stopSubtree() = traverseSubtreeForTickers { it.stop() }
}

abstract class MetaBehaviour(
    entity: GameEntity,
    var childBehaviour: Behaviour,
) : Behaviour(entity) {

    override fun traverseSubtreeForTickers(onEach: (Behaviour) -> Unit) {
        onEach(this)
        childBehaviour.traverseSubtreeForTickers(onEach)
    }

    final override fun tickersGroup(tickClass: KClass<out Tick>) = childBehaviour.tickersGroup(tickClass)
}
