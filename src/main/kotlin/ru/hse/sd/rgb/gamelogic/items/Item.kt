package ru.hse.sd.rgb.gamelogic.items

import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance

abstract class Item  {

    interface ViewItem {
        fun getSwingAppearance(): SwingUnitAppearance

        fun getDescription(): String
    }

    abstract val viewItem: ViewItem
    abstract fun getNewItemEntity(cell: Cell): ItemEntity
    abstract suspend fun use()
    abstract val isReusable: Boolean
    abstract val description: String
}

