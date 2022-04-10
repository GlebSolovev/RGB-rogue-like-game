package ru.hse.sd.rgb.views

import ru.hse.sd.rgb.Direction
import ru.hse.sd.rgb.Messagable
import ru.hse.sd.rgb.Message
import ru.hse.sd.rgb.entities.common.GameEntity

abstract class View : Messagable() {

    protected abstract var state: ViewState

    protected val movementListeners = mutableSetOf<Messagable>()
    protected val inventoryListeners = mutableSetOf<Messagable>()

    data class SubscribeToMovement(val listener: Messagable) : Message()
    data class SubscribeToInventory(val listener: Messagable) : Message()

    override suspend fun handleMessage(m: Message) {
        when (m) {
            is SubscribeToMovement -> movementListeners.add(m.listener)
            is SubscribeToInventory -> inventoryListeners.add(m.listener)
            is EntityMoved -> drawables[m.gameEntity] = m.nextSnapshot
            else -> state = state.next(m)
        }
    }

    protected val drawables = mutableMapOf<GameEntity, GameEntityViewSnapshot>()

    protected sealed class ViewState {
        abstract fun next(m: Message): ViewState
    }

    protected abstract class PlayingState : ViewState()
    protected abstract class PlayingInventoryState : ViewState()
    protected abstract class LoadingState : ViewState()
}

data class UserMoved(val dir: Direction) : Message()
class UserToggledInventory : Message()

data class EntityMoved(val gameEntity: GameEntity, val nextSnapshot: GameEntityViewSnapshot) : Message()
