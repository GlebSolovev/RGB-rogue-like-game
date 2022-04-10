package ru.hse.sd.rgb.entities

import ru.hse.sd.rgb.*
import ru.hse.sd.rgb.entities.common.*
import ru.hse.sd.rgb.views.*

class Hero : GameEntity() {

    inner class PhysicalHero : PhysicalEntity() {
        override val isSolid = true
    }

    inner class ViewHero : ViewEntity() {
        override fun convert(units: Set<GameUnit>): GameEntityViewSnapshot {
            return units.map {
                object : ViewUnit(it) {
                    override fun getSwingAppearance() =
                        SwingEntityAppearance(SwingUnitShape.CIRCLE, it.gameColor.toSwingColor())
                }
            }.toSet()
        }

        override fun applyMessageToAppearance(m: Message) = when (m) {
            else -> ignore
        }
    }

    override val units: Set<GameUnit> = setOf(
        GameUnit(this, Cell(0, 0), GameColor(255, 0, 0), true),
        GameUnit(this, Cell(1, 0), GameColor(0, 255, 0), true),
        GameUnit(this, Cell(0, 1), GameColor(255, 0, 255), true),
        GameUnit(this, Cell(1, 1), GameColor(255, 255, 255), true)
    )
    override val physicalEntity = PhysicalHero()
    override val viewEntity = ViewHero()

    override suspend fun handleMessage(m: Message) {
        when (m) {
            is UserMoved -> {
                val moved = controller.physics.tryMove(this, m.dir)
                if (moved) controller.view.receive(EntityMoved(this, viewEntity.convert(units)))
            }
        }
    }

}
