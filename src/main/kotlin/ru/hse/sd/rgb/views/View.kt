package ru.hse.sd.rgb.views

import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.*
import ru.hse.sd.rgb.utils.messaging.messages.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

typealias DrawablesMap = ConcurrentHashMap<GameEntity, GameEntityViewSnapshot>

abstract class View : Messagable() {

    open fun initialize() {}

    // swing draws from internal thread
    protected abstract val state: AtomicReference<ViewState>

    protected val movementListeners = mutableSetOf<Messagable>()
    protected val inventoryListeners = mutableSetOf<Messagable>()
    protected val quitListeners = mutableSetOf<Messagable>()

    final override suspend fun handleMessage(m: Message) {
        when (m) {
            is SubscribeToMovement -> movementListeners.add(m.listener)
            is SubscribeToInventory -> inventoryListeners.add(m.listener)
            is SubscribeToQuit -> quitListeners.add(m.listener)
            is UnsubscribeFromMovement -> movementListeners.remove(m.listener)
            is UnsubscribeFromInventory -> movementListeners.remove(m.listener)
            else -> state.set(state.get().next(m))
        }
    }

    protected abstract class ViewState {
        abstract fun next(m: Message): ViewState
    }

    protected abstract class PlayingState(
        // panels may be accessed from different thread
        protected val drawables: DrawablesMap
    ) : ViewState()

    protected abstract class PlayingInventoryState : ViewState()
    protected abstract class LoadingState : ViewState()
}
