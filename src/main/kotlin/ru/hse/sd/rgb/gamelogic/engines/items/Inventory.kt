package ru.hse.sd.rgb.gamelogic.engines.items

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.utils.structures.*
import java.awt.Color

data class InventorySwingAppearance(
    val bgAlpha: Int, // from 0 (transparent) to 255 (opaque)
    val gridColor: Color,
    val highlightColor: Color,
    val scale: Double,
    val itemBgAlpha: Int,
    val equippedItemHighlightAlpha: Int
)

data class InventoryViewSnapshot(
    val itemsGrid: Grid2D<Item.ViewItem?>,
    val selectedCell: Cell,

    val swingAppearance: InventorySwingAppearance
)

// should be not thread-safe, only its holder entity operates with it
class Inventory(
    private val w: Int,
    private val h: Int,
) {

    companion object {
        val DEFAULT_SWING_APPEARANCE = InventorySwingAppearance(100, Color.WHITE, Color.YELLOW, 0.9, 200, 128)
    }

    private var selectedCell: Cell = Cell(0, 0)
    private val items: Grid2D<Item?> = Grid2D(w, h) { _, _ -> null }

    inner class ViewInventory {
        private val swingAppearance = DEFAULT_SWING_APPEARANCE

        fun takeViewSnapshot() = InventoryViewSnapshot(
            items.map { it?.viewItem },
            selectedCell,
            swingAppearance
        )
    }

    val viewInventory = ViewInventory()

    fun isNotFull(): Boolean = findEmptyCell() != null

    fun addItem(item: Item): Boolean {
        val cell = findEmptyCell() ?: throw IllegalArgumentException("can't add item into full inventory")
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

    suspend fun useCurrent(): Boolean {
        val item = items[selectedCell] ?: return false
        item.use()
        if (!item.isReusable) items[selectedCell] = null
        return true
    }

    // returns true if item was dropped
    suspend fun dropCurrent(cell: Cell): Boolean {
        val item = items[selectedCell] ?: return false

        if (controller.itemsEngine.tryDrop(item, cell)) {
            items[selectedCell] = null
            return true
        }
        return false
    }
}