package ru.hse.sd.rgb.gamelogic.engines.items

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.structures.Cell

class ItemsEngine {

    // TODO: itemEntity.getNewItem(pickerEntity) thread-safety is needed so far :(
    suspend fun tryPick(pickerEntity: GameEntity, itemEntity: ItemEntity): Item? {
        val item = itemEntity.getNewItem(pickerEntity)
        if (item != null) controller.creation.die(itemEntity)
        return item
    }

    suspend fun tryDrop(item: Item, cell: Cell): Boolean {
        if (item is ReusableItem && item.isEquipped()) return false
        val itemEntity = item.getNewItemEntity(cell)
        return controller.creation.tryAddToWorld(itemEntity)
    }

}
