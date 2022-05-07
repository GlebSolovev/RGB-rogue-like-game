package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gamelogic.entities.*
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.views.*
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class Wall(colorCells: Set<ColorCellNoHp>) : GameEntity(colorCells) {

    constructor(color: RGB, cell: Cell) : this(setOf(ColorCellNoHp(color, cell)))

    override val viewEntity: ViewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit): ViewUnit = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.SQUARE)
        }

        override fun applyMessageToAppearance(m: Message) = ignore
    }

    override val physicalEntity: PhysicalEntity = object : PhysicalEntity() {
        override val isSolid: Boolean = true
        override fun getUnitDirection(unit: GameUnit, dir: Direction): Direction = Direction.NOPE
    }

    override val fightEntity = object : FightEntity() {
        override fun isUnitActive(unit: GameUnit): Boolean = false
    }

    override suspend fun handleGameMessage(m: Message) = ignore
}