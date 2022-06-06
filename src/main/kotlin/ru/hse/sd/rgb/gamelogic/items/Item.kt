package ru.hse.sd.rgb.gamelogic.items

import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance

abstract class Item(protected val holder: GameEntity) {

    companion object {
        const val DEFAULT_ITEM_INVENTORY_VIEW_UNIT_SCALE = 0.9
    }

    abstract inner class ViewItem {
        abstract fun getSwingAppearance(): SwingUnitAppearance

        abstract val color: RGB
        abstract val description: String
    }

    abstract val viewItem: ViewItem
    abstract fun getNewItemEntity(cell: Cell): BasicItemEntity
    abstract suspend fun use()
    abstract val isReusable: Boolean
}
