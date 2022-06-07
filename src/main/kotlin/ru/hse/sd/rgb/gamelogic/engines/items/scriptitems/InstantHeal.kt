package ru.hse.sd.rgb.gamelogic.engines.items.scriptitems

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.items.BasicItemEntity
import ru.hse.sd.rgb.gamelogic.engines.items.NonReusableItem
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class InstantHealItem(holder: GameEntity, private val healAmount: Int) : NonReusableItem(holder) {

    companion object {
        val HEAL_GREEN = RGB(30, 210, 30)
        val SWING_VIEW_SHAPE = SwingUnitShape.PLUS
    }

    override val viewItem = object : ViewNonReusableItem() {
        override fun getSwingAppearance() =
            SwingUnitAppearance(SWING_VIEW_SHAPE, null, DEFAULT_ITEM_INVENTORY_VIEW_UNIT_SCALE)

        override val color = HEAL_GREEN
        override val description: String = "Heals $healAmount HP instantly"
    }

    override fun getNewItemEntity(cell: Cell): BasicItemEntity = InstantHealEntity(cell, healAmount)

    override suspend fun use() {
        for (unit in holder.units) { // TODO: select unit
            controller.fighting.heal(unit, healAmount)
        }
    }
}

class InstantHealEntity(cell: Cell, private val healAmount: Int) : BasicItemEntity(cell, InstantHealItem.HEAL_GREEN) {

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit) = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(InstantHealItem.SWING_VIEW_SHAPE, null)
        }
    }

    override fun getNewItemUnconditionally(picker: GameEntity) = InstantHealItem(picker, healAmount)
}
