package ru.hse.sd.rgb.gamelogic.engines.items.scriptitems

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.items.BasicItemEntity
import ru.hse.sd.rgb.gamelogic.engines.items.Item
import ru.hse.sd.rgb.gamelogic.engines.items.ItemEntity
import ru.hse.sd.rgb.gamelogic.engines.items.ReusableItem
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.messaging.messages.EntityUpdated
import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.utils.structures.invert
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class ColorInverterItem(holder: GameEntity) : ReusableItem(holder) {

    companion object {
        val INVERTER_COLOR = RGB(240, 240, 240)
        val SWING_VIEW_SHAPE = SwingUnitShape.CROSS
    }

    override val viewItem: ViewItem = object : ViewReusableItem() {
        override fun getSwingAppearance() =
            SwingUnitAppearance(SWING_VIEW_SHAPE, null, DEFAULT_ITEM_INVENTORY_VIEW_UNIT_SCALE)

        override val color = INVERTER_COLOR
        override val description: String = "When equipped, inverts current RGB"
    }

    override fun getNewItemEntity(cell: Cell): ItemEntity = ColorInverterEntity(cell)

    private suspend fun invertColors() {
        // TODO: select unit
        for (unit in holder.units) {
            controller.fighting.changeRGB(unit, unit.gameColor.invert())
        }
        controller.view.receive(EntityUpdated(holder))
    }

    override suspend fun equip() = invertColors()

    override suspend fun unequip() = invertColors()
}

class ColorInverterEntity(cell: Cell) : BasicItemEntity(cell, ColorInverterItem.INVERTER_COLOR) {

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit) = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(ColorInverterItem.SWING_VIEW_SHAPE, null)
        }
    }

    override fun getNewItemUnconditionally(picker: GameEntity): Item = ColorInverterItem(picker)
}
