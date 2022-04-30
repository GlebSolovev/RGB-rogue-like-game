package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.gamelogic.engines.fight.GameColor
import ru.hse.sd.rgb.utils.Message
import ru.hse.sd.rgb.gamelogic.entities.ColorHpCell
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.Direction
import ru.hse.sd.rgb.utils.ignore
import ru.hse.sd.rgb.views.*
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class Wall(colorHpCells: Set<ColorHpCell>) : GameEntity(colorHpCells) {

    constructor(color: GameColor, hp: Int, cell: Cell) : this(setOf(ColorHpCell(color, hp, cell)))

    override val physicalEntity: PhysicalEntity = object : PhysicalEntity() {
        override val isSolid: Boolean = true
        override fun getUnitDirection(unit: GameUnit, dir: Direction): Direction = dir // TODO: special empty direction
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