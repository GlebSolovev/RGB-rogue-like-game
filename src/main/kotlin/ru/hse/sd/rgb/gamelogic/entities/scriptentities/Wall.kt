package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.entities.*
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.views.*
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class Wall(colorHpCells: Set<ColorHpCell>) : GameEntity(colorHpCells) {

    constructor(color: RGB, hp: Int, cell: Cell) : this(setOf(ColorHpCell(color, hp, cell)))

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
        when (m) {
            is ReceivedAttack -> {
                if (m.isFatal) controller.creation.die(this)
            }
            else -> ignore
        }
    }
}