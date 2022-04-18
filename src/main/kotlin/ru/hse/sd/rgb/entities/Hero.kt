package ru.hse.sd.rgb.entities

import ru.hse.sd.rgb.*
import ru.hse.sd.rgb.entities.common.*
import ru.hse.sd.rgb.views.*
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class Hero(colorCells: Set<ColorCell>) : GameEntity(colorCells) {

    inner class PhysicalHero : PhysicalEntity() {
        override val isSolid = true
    }

    inner class ViewHero : ViewEntity() {
        override fun convertUnit(unit: GameUnit): ViewUnit = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.SQUARE)
        }
    }

    override val physicalEntity = PhysicalHero()
    override val viewEntity = ViewHero()

    override fun onGameStart() {
        controller.view.receive(View.SubscribeToMovement(this))
    }

    override suspend fun handleGameMessage(m: Message) {
        when (m) {
            is UserMoved -> {
                val moved = controller.physics.tryMove(this, m.dir)
                if (moved) controller.view.receive(EntityMoved(this))
            }
        }
    }

}
