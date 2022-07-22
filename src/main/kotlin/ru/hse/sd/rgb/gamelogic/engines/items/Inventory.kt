package ru.hse.sd.rgb.gamelogic.engines.items

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gameloaders.InventoryDescription
import ru.hse.sd.rgb.gamelogic.engines.experience.DetailedExperience
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.HpGameUnit
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

data class UnitHp(val hp: Int?, val maxHp: Int?)
data class InventoryHolderStats(
    val unitsHp: List<UnitHp>,
    val unitsRgb: List<RGB>,
    val unitsColorName: List<String>,
    val experience: DetailedExperience
)

data class InventoryViewSnapshot(
    val itemsGrid: Grid2D<Item.ViewItem?>,
    val selectedCell: Cell,
    val swingAppearance: InventorySwingAppearance,
    val holderStats: InventoryHolderStats
)

// doesn't need to be thread-safe, only its holder entity operates with it
class Inventory(
    private val holder: GameEntity,
    private val items: Grid2D<Item?>,
    private var selectedCell: Cell = Cell(0, 0),
) {
    companion object {
        val DEFAULT_SWING_APPEARANCE = InventorySwingAppearance(100, Color.WHITE, Color.YELLOW, 0.9, 200, 128)
    }

    private val w = items.w
    private val h = items.h

    inner class ViewInventory {
        private val swingAppearance = DEFAULT_SWING_APPEARANCE

        suspend fun takeViewSnapshot() = InventoryViewSnapshot(
            items.map { it?.viewItem },
            selectedCell,
            swingAppearance,
            InventoryHolderStats(
                holder.units.map {
                    if (it is HpGameUnit) UnitHp(it.hp, it.maxHp) else UnitHp(null, null)
                },
                holder.units.map { it.gameColor },
                holder.units.map { controller.fighting.getBaseColorName(it) },
                controller.experience.getDetailedExperience(holder)!!
            )
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

    // TODO: handle equipping-unequipping many times a second
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

    fun extractPersistence() = InventoryPersistence(
        items.map { it?.extractPersistence() },
        selectedCell
    )
}

class InventoryPersistence(
    private val itemsPersistence: Grid2D<ItemPersistence?>,
    private val selectedCell: Cell,
) {
    constructor(w: Int, h: Int) : this(Grid2D(w, h) { _, _ -> null }, Cell(0, 0))

    fun convertToInventory(holder: GameEntity) = Inventory(
        holder,
        itemsPersistence.map { it?.convertToItem(holder) },
        selectedCell
    )

    val description = InventoryDescription(itemsPersistence.w, itemsPersistence.h)
}
