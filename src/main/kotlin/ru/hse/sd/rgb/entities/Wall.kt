package ru.hse.sd.rgb.entities

import ru.hse.sd.rgb.Cell
import ru.hse.sd.rgb.GameColor
import ru.hse.sd.rgb.Message
import ru.hse.sd.rgb.entities.common.GameEntity
import ru.hse.sd.rgb.entities.common.GameUnit
import ru.hse.sd.rgb.entities.common.PhysicalEntity
import ru.hse.sd.rgb.entities.common.ViewEntity
import ru.hse.sd.rgb.ignore
import ru.hse.sd.rgb.views.*

class Wall(x: Int, y: Int) : GameEntity() {
    override val units: Set<GameUnit> = setOf(GameUnit(this, Cell(x, y), GameColor(150, 150, 150), true))
    override val physicalEntity: PhysicalEntity = object : PhysicalEntity() {
        override val isSolid: Boolean = true
    }
    override val viewEntity: ViewEntity = object : ViewEntity() {
        override fun convert(units: Set<GameUnit>): GameEntityViewSnapshot {
            return units.map {
                object : ViewUnit(it) {
                    override fun getSwingAppearance(): SwingEntityAppearance {
                        return SwingEntityAppearance(
                            SwingUnitShape.SQUARE,
                            it.gameColor.toSwingColor() // TODO: too decoupled (?)
                        )
                    }
                }
            }.toSet()
        }

        // TODO: creation mechanism

        override fun applyMessageToAppearance(m: Message) {
            ignore
        }
    }

    override suspend fun handleMessage(m: Message) {
        ignore
    }
}