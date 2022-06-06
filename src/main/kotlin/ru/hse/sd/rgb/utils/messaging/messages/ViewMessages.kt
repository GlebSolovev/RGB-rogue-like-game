package ru.hse.sd.rgb.utils.messaging.messages

import ru.hse.sd.rgb.gameloaders.LevelDescription
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.items.Inventory
import ru.hse.sd.rgb.utils.messaging.Messagable
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.Tick
import ru.hse.sd.rgb.views.GameEntityViewSnapshot

// messages to control View

class ViewTick : Tick()

data class SubscribeToMovement(val listener: Messagable) : Message()
data class SubscribeToInventory(val listener: Messagable) : Message()
data class SubscribeToQuit(val listener: Messagable) : Message()

data class UnsubscribeFromMovement(val listener: Messagable) : Message()
data class UnsubscribeFromInventory(val listener: Messagable) : Message()

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
