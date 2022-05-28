package ru.hse.sd.rgb.gamelogic.items

import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.utils.structures.Grid2D
import java.awt.Color

data class InventorySwingAppearance(
    val bgAlpha: Int, // from 0 (transparent) to 255 (opaque)
    val gridColor: Color,
    val highlightColor: Color,
    val scale: Double,
    val itemBgAlpha: Int,
)

data class InventoryViewSnapshot(
    val itemsGrid: Grid2D<Item.ViewItem?>,
    val selectedCell: Cell,

    val swingAppearance: InventorySwingAppearance
)

class Inventory(
    private val w: Int,
    private val h: Int,
) {
    private var selectedCell: Cell = Cell(0, 0)
    private val items: Grid2D<Item?> = Grid2D(w, h) { _, _, -> null }

    inner class ViewInventory {
        private val swingAppearance = InventorySwingAppearance(100, Color.WHITE, Color.YELLOW, 0.9, 200)

        fun takeViewSnapshot() = InventoryViewSnapshot(
            items.map { it?.viewItem }, // TODO: should be concurrent?
            selectedCell,
            swingAppearance
        )

        fun applyMessageToAppearance(m: Message) {} // TODO?
    }

    val viewInventory = ViewInventory()

    // return false if inventory is full, true otherwise
    fun addItem(item: Item): Boolean {
        val cell = findEmptyCell() ?: return false
        items[cell] = item
        return true
    }

    private fun findEmptyCell(): Cell? {
        for (x in 0 until w) {
            for (y in 0 until h) {
                val cell = Cell(x, y)
                if (items[cell] == null) return cell
            }
        }
        return null
    }

    fun moveSelection(dir: Direction) {
        val nextCell = selectedCell + dir.toShift()
        if (nextCell.x in 0 until w && nextCell.y in 0 until h)
            selectedCell = nextCell
    }

    fun useCurrent(): Boolean {
        val item = items[selectedCell] ?: return false
        item.use()
        if (!item.isReusable) items[selectedCell] = null
        return true
    }

    fun dropCurrent(): Item? {
        val item = items[selectedCell] ?: return null
        items[selectedCell] = null
        return item
    }
}
