package ru.hse.sd.rgb.gamelogic.items.scriptitems

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.gamelogic.items.Item
import ru.hse.sd.rgb.gamelogic.items.ItemEntity
import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class InstantHealItem(color: RGB, holder: GameEntity, private val healAmount: Int) : Item(color, holder) {
    override val viewItem = object : ViewItem(this) {
        override fun getSwingAppearance() = SwingUnitAppearance(SwingUnitShape.PLUS, 0.9)
    }

    override fun getNewItemEntity(cell: Cell): ItemEntity {
        TODO("Not yet implemented")
    }

    override suspend fun use() {
        for (unit in holder.units) {// TODO: select unit
            controller.fighting.heal(unit, healAmount)
        }
    }

    override val isReusable = false
    override val description = "Heals $healAmount HP instantly"
}

private val HEAL_GREEN = RGB(30, 210, 30)

class InstantHealEntity(cell: Cell, private val healAmount: Int) : ItemEntity(cell, HEAL_GREEN) {
    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit) = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.PLUS)
        }
    }

    override fun getNewItem(picker: GameEntity) = InstantHealItem(units.first().gameColor, picker, healAmount)
}
