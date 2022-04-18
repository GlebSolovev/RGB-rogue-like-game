package ru.hse.sd.rgb.views

import ru.hse.sd.rgb.Direction
import ru.hse.sd.rgb.Messagable
import ru.hse.sd.rgb.Message
import ru.hse.sd.rgb.entities.common.GameEntity
import ru.hse.sd.rgb.levelloading.LevelDescription
import java.util.concurrent.atomic.AtomicReference

abstract class View : Messagable() {

    // swing draws from internal thread
    protected abstract val state: AtomicReference<ViewState>

    protected val movementListeners = mutableSetOf<Messagable>()
    protected val inventoryListeners = mutableSetOf<Messagable>()
    protected val quitListeners = mutableSetOf<Messagable>()

    // handled in single thread
    data class SubscribeToMovement(val listener: Messagable) : Message()
    data class SubscribeToInventory(val listener: Messagable) : Message()
    data class SubscribeToQuit(val listener: Messagable) : Message()

    final override suspend fun handleMessage(m: Message) {
        when (m) {
            is SubscribeToMovement -> movementListeners.add(m.listener)
            is SubscribeToInventory -> inventoryListeners.add(m.listener)
            is SubscribeToQuit -> quitListeners.add(m.listener)
            else -> state.set(state.get().next(m))
        }
    }

    protected sealed class ViewState {
        abstract fun next(m: Message): ViewState
    }

    protected abstract class PlayingState(
        protected val drawables: MutableMap<GameEntity, GameEntityViewSnapshot>,
        protected val wGrid: Int,
        protected val hGrid: Int,
        protected val bgColor: RGB
    ) : ViewState()

    protected abstract class PlayingInventoryState : ViewState()
    protected abstract class LoadingState : ViewState()
}

data class UserMoved(val dir: Direction) : Message()
class UserToggledInventory : Message()
class UserQuit : Message()

data class EntityMoved(val gameEntity: GameEntity) : Message() {
    val nextSnapshot: GameEntityViewSnapshot = gameEntity.viewEntity.takeViewSnapshot()
}

data class GameViewStarted(val level: LevelDescription) : Message() {
    val drawables: MutableMap<GameEntity, GameEntityViewSnapshot> =
        level.entities.associateWith { it.viewEntity.takeViewSnapshot() }.toMutableMap()
}
