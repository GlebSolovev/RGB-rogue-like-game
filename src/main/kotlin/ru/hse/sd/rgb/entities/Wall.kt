package ru.hse.sd.rgb.entities

import ru.hse.sd.rgb.Cell
import ru.hse.sd.rgb.GameColor
import ru.hse.sd.rgb.Message
import ru.hse.sd.rgb.entities.common.ColorCell
import ru.hse.sd.rgb.entities.common.GameEntity
import ru.hse.sd.rgb.entities.common.GameUnit
import ru.hse.sd.rgb.ignore
import ru.hse.sd.rgb.views.*
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class Wall(colorCells: Set<ColorCell>) : GameEntity(colorCells) {

    constructor(cell: Cell, color: GameColor) : this(setOf(ColorCell(cell, color)))

    override val physicalEntity: PhysicalEntity = object : PhysicalEntity() {
        override val isSolid: Boolean = true
    }
    override val viewEntity: ViewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit): ViewUnit = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.SQUARE)
        }

        override fun applyMessageToAppearance(m: Message) {
            ignore
        }
    }

    override suspend fun handleGameMessage(m: Message) {
        ignore
    }
}