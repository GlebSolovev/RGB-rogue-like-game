package ru.hse.sd.rgb.gamelogic.items.scriptitems

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.gamelogic.items.Item
import ru.hse.sd.rgb.gamelogic.items.ItemEntity
import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.utils.messaging.messages.EntityUpdated
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.utils.structures.RGBDelta
import ru.hse.sd.rgb.utils.structures.plus
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class ColorModificationItem(
    color: RGB,
    holder: GameEntity,
    private val rgbDelta: RGBDelta, // TODO: isAbsolute
) : Item(color, holder) {

    override val viewItem = object : ViewItem(this) {
        override fun getSwingAppearance() = SwingUnitAppearance(SwingUnitShape.SPINNING_SQUARE, 0.9)
    }

    override val isReusable = false
    override suspend fun use() {
        // TODO: select unit
        for (unit in holder.units) {
            controller.fighting.changeRGB(unit, unit.gameColor + rgbDelta)
        }
        controller.view.receive(EntityUpdated(holder))
    }

    override val description = run {
        fun colorToText(dc: Int) = if (dc > 0) "+$dc" else "$dc"
        val (dr, dg, db) = rgbDelta
        val rt = colorToText(dr)
        val gt = colorToText(dg)
        val bt = colorToText(db)
        "R: $rt   G: $gt   B: $bt"
    }

    override fun getNewItemEntity(cell: Cell): ItemEntity = ColorModificationEntity(cell, rgbDelta)
}

class ColorModificationEntity(cell: Cell, private val rgbDelta: RGBDelta) : ItemEntity(cell, rgbDelta.saturate()) {
    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit) = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.SPINNING_SQUARE)
        }
    }

    override fun getNewItem(picker: GameEntity) =
        ColorModificationItem(this.units.first().gameColor, picker, rgbDelta)
}
