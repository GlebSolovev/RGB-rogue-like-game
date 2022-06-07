package ru.hse.sd.rgb.gamelogic.engines.items.scriptitems

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.items.BasicItemEntity
import ru.hse.sd.rgb.gamelogic.engines.items.NonReusableItem
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.messaging.messages.EntityUpdated
import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.utils.structures.RGBDelta
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class ColorModificationItem(
    holder: GameEntity,
    private val rgbDelta: RGBDelta
) : NonReusableItem(holder) {

    companion object {
        val SWING_VIEW_SHAPE = SwingUnitShape.SPINNING_SQUARE
    }

    override val viewItem = object : ViewNonReusableItem() {
        override fun getSwingAppearance() =
            SwingUnitAppearance(SWING_VIEW_SHAPE, null, DEFAULT_ITEM_INVENTORY_VIEW_UNIT_SCALE)

        override val color: RGB = this@ColorModificationItem.rgbDelta.convertToViewRGB()

        override val description = run {
            fun colorToText(dc: Int) = if (dc > 0) "+$dc" else "$dc"
            val (dr, dg, db) = rgbDelta
            val rt = colorToText(dr)
            val gt = colorToText(dg)
            val bt = colorToText(db)
            "R: $rt   G: $gt   B: $bt"
        }
    }

    override suspend fun use() {
        // TODO: select unit
        for (unit in holder.units) {
            controller.fighting.changeRGB(unit, rgbDelta)
        }
        controller.view.receive(EntityUpdated(holder))
    }

    override fun getNewItemEntity(cell: Cell): BasicItemEntity = ColorModificationEntity(cell, rgbDelta, true)
}

class ColorModificationEntity(cell: Cell, private val rgbDelta: RGBDelta, respawned: Boolean = false) :
    BasicItemEntity(cell, rgbDelta.convertToViewRGB(), respawned) {

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit) = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(ColorModificationItem.SWING_VIEW_SHAPE, null)
        }
    }

    override fun getNewItemUnconditionally(picker: GameEntity) = ColorModificationItem(picker, rgbDelta)
}
