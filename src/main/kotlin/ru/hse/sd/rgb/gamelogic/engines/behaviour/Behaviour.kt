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

    fun startTickers() {
        traverseTickers { ticker ->
            ticker.start()
            tickersGroup(ticker.tick::class).add(ticker)
        }
    }

    fun stopTickers() {
        traverseTickers { ticker ->
            ticker.stop()
            tickersGroup(ticker.tick::class).remove(ticker)
        }
    }

    abstract fun traverseSubtree(onEach: (Behaviour) -> Unit)

    fun startSubtreeTickers() = traverseSubtree { it.startTickers() }
    fun stopSubtreeTickers() = traverseSubtree { it.stopTickers() }
}

abstract class MetaBehaviour(
    entity: GameEntity,
    var childBehaviour: Behaviour,
) : Behaviour(entity) {

    final override fun traverseSubtree(onEach: (Behaviour) -> Unit) {
        onEach(this)
        childBehaviour.traverseSubtree(onEach)
    }

    final override fun tickersGroup(tickClass: KClass<out Tick>) = childBehaviour.tickersGroup(tickClass)
}
