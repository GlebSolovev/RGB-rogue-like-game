package ru.hse.sd.rgb.views

import ru.hse.sd.rgb.utils.Direction
import ru.hse.sd.rgb.utils.Messagable
import ru.hse.sd.rgb.utils.Message
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gameloaders.LevelDescription
import ru.hse.sd.rgb.gamelogic.engines.items.Inventory
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

    // handled in single thread
    data class SubscribeToMovement(val listener: Messagable) : Message()
    data class SubscribeToInventory(val listener: Messagable) : Message()
    data class SubscribeToQuit(val listener: Messagable) : Message()

    data class UnsubscribeFromMovement(val listener: Messagable) : Message()
    data class UnsubscribeFromInventory(val listener: Messagable) : Message()

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

    protected sealed class ViewState {
        abstract fun next(m: Message): ViewState
    }

    protected abstract class PlayingState(
        // panels may be accessed from different thread
        protected val drawables: DrawablesMap
    ) : ViewState()

    protected abstract class PlayingInventoryState : ViewState()
    protected abstract class LoadingState : ViewState()
}

data class UserMoved(val dir: Direction) : Message()
class UserToggledInventory : Message()
class UserQuit : Message()
class UserSelect : Message()
class UserDrop : Message()

data class EntityUpdated(val gameEntity: GameEntity) : Message() {
    val newSnapshot: GameEntityViewSnapshot = gameEntity.viewEntity.takeViewSnapshot()
}

data class EntityRemoved(val gameEntity: GameEntity) : Message()

data class GameViewStarted(val level: LevelDescription) : Message()

class InventoryOpened(inventory: Inventory) : Message() {
    val invSnapshot = inventory.viewInventory.takeViewSnapshot()
}

class InventoryUpdated(inventory: Inventory) : Message() {
    val invSnapshot = inventory.viewInventory.takeViewSnapshot()
}

class InventoryClosed : Message()
