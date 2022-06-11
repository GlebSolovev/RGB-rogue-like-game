package ru.hse.sd.rgb.gamelogic.engines.items

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.engines.creation.CreationEngine
import ru.hse.sd.rgb.utils.structures.Cell

/**
 * Class for converting [Item] into [ItemEntity] and vice versa.
 */
class ItemsEngine {

    /**
     * Tries to pick [itemEntity] as an [Item].
     *
     * If [itemEntity] is not ready to be picked, does nothing and returns null.
     * Otherwise, converts [itemEntity] into an [Item] and then kills it using
     * [CreationEngine].
     *
     * @param pickerEntity The [GameEntity] that is trying to pick the item.
     * @param itemEntity The [ItemEntity] being picked.
     * @return The picked [Item] if it was actually picked, otherwise null.
     */
    // TODO: itemEntity.getNewItem(pickerEntity) thread-safety is needed so far :(
    // TODO: lock pickerEntity to prevent multiple pick
    suspend fun tryPick(pickerEntity: GameEntity, itemEntity: ItemEntity): Item? {
        val item = itemEntity.getNewItem(pickerEntity)
        if (item != null) controller.creation.die(itemEntity)
        return item
    }

    /**
     * Tries to drop [item] as an [ItemEntity] at [cell].
     *
     * If [item] is currently equipped, does nothing and returns false. Otherwise,
     * converts [item] to [ItemEntity] and tries to spawn it using [CreationEngine].
     *
     * @param item The item being dropped.
     * @param cell The cell at which to spawn converted [ItemEntity].
     * @return true if the converted [ItemEntity] successfully spawned, false otherwise.
     */
    suspend fun tryDrop(item: Item, cell: Cell): Boolean {
        if (item is ReusableItem && item.isEquipped()) return false
        val itemEntity = item.getNewItemEntity(cell)
        return controller.creation.tryAddToWorld(itemEntity)
    }

}
