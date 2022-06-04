package ru.hse.sd.rgb.gamelogic.items

import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance

abstract class Item(protected val color: RGB, protected val holder: GameEntity) {

    abstract class ViewItem(parent: Item) {
        abstract fun getSwingAppearance(): SwingUnitAppearance

        val color = parent.color
        val description = parent.description
    }

    abstract val viewItem: ViewItem
    abstract fun getNewItemEntity(cell: Cell): ItemEntity
    abstract suspend fun use()
    abstract val isReusable: Boolean
    abstract val description: String
}
