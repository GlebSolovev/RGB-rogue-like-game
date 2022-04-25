package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.entities.*
import ru.hse.sd.rgb.utils.Direction
import ru.hse.sd.rgb.utils.Message
import ru.hse.sd.rgb.utils.unreachable
import ru.hse.sd.rgb.views.*
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class Hero(colorHpCells: Set<ColorHpCell>) : GameEntity(colorHpCells) {

    inner class PhysicalHero : PhysicalEntity() {
        override val isSolid = true
        override fun getUnitDirection(unit: GameUnit, dir: Direction): Direction = dir
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
            is CollidedWith -> {
                controller.fighting.attack(m.myUnit, m.otherUnit)
            }
            is ColorTick -> {
//                addArgs = Struct { attackTarget: NEAREST, healTarget: LOW_HP }
//                state.get(unit) -> fightEntity.get(unit)
//                controller.fighting.update(m.unit, addArgs)
                // TODO: unit update in fighting logic
            }
            else -> unreachable
        }
    }

}
