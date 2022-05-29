package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.SimpleBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.State
import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.entities.*
import ru.hse.sd.rgb.utils.Direction
import ru.hse.sd.rgb.utils.messaging.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.*
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

    override var behaviour: Behaviour = LaserPartDefaultBehaviour()
    override val behaviourEntity = SingleBehaviourEntity(behaviour)

    private inner class LaserPartDefaultBehaviour : SimpleBehaviour(this) {

        // TODO: magic number 5
        private val continueTicker = Ticker(6, this@LaserPart, ContinueTick())
        private val dieTicker = Ticker(persistMillis, this@LaserPart, DieTick())
        private var didContinue = false
        private var didDie = false

        override var state = object : State() {

            override suspend fun handleReceivedAttack(message: ReceivedAttack): State = this

            override suspend fun handleCollidedWith(message: CollidedWith): State {
                controller.fighting.attack(message.myUnit, message.otherUnit)
                return this
            }

            override suspend fun handleColorTick(tick: ColorTick): State = this

            override suspend fun handleMoveTick(): State {
                if (controller.physics.tryMove(this@LaserPart, dir))
                    controller.view.receive(EntityUpdated(this@LaserPart))
                else
                    didContinue = true // prevent stuck parts from cloning
                return this
            }

            override suspend fun handleDieTick(): State {
                if (didDie) return this
                continueTicker.stop()
                dieTicker.stop()
                controller.creation.die(this@LaserPart)
                didDie = true
                return this
            }

            override suspend fun handleContinueTick(): State {
                if (didContinue) return this
                val clone = clone()
                if (controller.creation.tryAddToWorld(clone)) {
                    controller.view.receive(EntityUpdated(clone))
                    clone.receive(MoveTick()) // TODO: rename MoveTick
                }
                didContinue = true
                continueTicker.stop()
                return this
            }
        }

        override fun startTickers() {
            continueTicker.start()
            dieTicker.start()
        }

        override fun stopTickers() {
            continueTicker.stop()
            dieTicker.stop()
        }
    }

    private fun clone(): LaserPart {
        val unit = units.first()
        return LaserPart(ColorCellNoHp(unit.gameColor, unit.cell), persistMillis, dir, teamId)
    }

}