package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.entities.*
import ru.hse.sd.rgb.utils.Direction
import ru.hse.sd.rgb.utils.Message
import ru.hse.sd.rgb.utils.Tick
import ru.hse.sd.rgb.utils.Ticker.Companion.createTicker
import ru.hse.sd.rgb.views.EntityUpdated
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class LaserPart(
    cell: ColorCellNoHp,
    private val persistMillis: Long,
    private val dir: Direction,
    private val teamId: Int,
) : GameEntity(setOf(cell)) {

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit) = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.RECTANGLE(dir))
        }
    }

    override val physicalEntity = object : PhysicalEntity() {
        override val isSolid = false
        override fun getUnitDirection(unit: GameUnit, dir: Direction) = dir
    }

    override val fightEntity = object : FightEntity() {
        override fun isUnitActive(unit: GameUnit) = false
        override val teamId = this@LaserPart.teamId
    }

    private class DieTick : Tick() // TODO: private ticks were bad for some reason?
    private class ContinueTick : Tick()

    val continueTicker = createTicker(6, ContinueTick()).also { it.start() }
    val dieTicker = createTicker(persistMillis, DieTick()).also { it.start() }
    private var didContinue = false
    private var didDie = false

    override suspend fun handleGameMessage(m: Message) {
        when (m) {
            is ContinueTick -> {
                if (didContinue) return
                val clone = clone()
                if (controller.creation.tryAddToWorld(clone)) {
                    controller.view.receive(EntityUpdated(clone))
                    clone.receive(MoveTick())
                }
                didContinue = true
                continueTicker.stop()
            }
            is MoveTick -> {
                if (controller.physics.tryMove(this, dir))
                    controller.view.receive(EntityUpdated(this))
                else
                    didContinue = true // prevent stuck parts from cloning
            }
            is CollidedWith -> {
                controller.fighting.attack(m.myUnit, m.otherUnit)
            }
            is DieTick -> {
                if (didDie) return
                continueTicker.stop()
                dieTicker.stop()
                controller.creation.die(this)
                didDie = true
            }
        }
    }

    private fun clone(): LaserPart {
        val unit = units.first()
        return LaserPart(ColorCellNoHp(unit.gameColor, unit.cell), persistMillis, dir, teamId)
    }

}