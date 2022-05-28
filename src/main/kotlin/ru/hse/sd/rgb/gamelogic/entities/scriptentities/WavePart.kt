package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.SimpleBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.State
import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.entities.*
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape
import ru.hse.sd.rgb.utils.Ticker.Companion.createTicker
import ru.hse.sd.rgb.utils.messaging.messages.*

class WavePart(
    cell: ColorCellNoHp,
    movePeriodMillis: Long,
    dir: Direction,
    teamId: Int,
) : GameEntity(setOf(cell)) {

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit) = object : ViewUnit(unit) {
            override val swingAppearance: SwingUnitAppearance
                get() = SwingUnitAppearance(SwingUnitShape.CIRCLE_HALF(dir))
        }
    }

    override val physicalEntity = object : PhysicalEntity() {
        override val isSolid = false
        override fun getUnitDirection(unit: GameUnit, dir: Direction) = dir
    }

    override val fightEntity = object : FightEntity() {
        override fun isUnitActive(unit: GameUnit) = false
        override val teamId = teamId
    }

    override var behaviour: Behaviour = WavePartDefaultBehaviour(movePeriodMillis, dir)
    override val behaviourEntity = SingleBehaviourEntity(behaviour)

    private inner class WavePartDefaultBehaviour(movePeriodMillis: Long, dir: Direction) : SimpleBehaviour() {

        val moveTicker = createTicker(movePeriodMillis, MoveTick()).also { it.start() }

        override var state = object : State() {

            override suspend fun handleReceivedAttack(message: ReceivedAttack): State {
                if (message.isFatal) controller.creation.die(this@WavePart)
                return this
            }

            override suspend fun handleCollidedWith(message: CollidedWith): State {
                if (message.otherUnit.parent.fightEntity.teamId == fightEntity.teamId) return this
                controller.fighting.attack(message.myUnit, message.otherUnit)
                controller.creation.die(this@WavePart)
                return this
            }

            override suspend fun handleColorTick(tick: ColorTick): State = this

            override suspend fun handleMoveTick(): State {
                val moved = controller.physics.tryMove(this@WavePart, dir)
                if (moved) controller.view.receive(EntityUpdated(this@WavePart))
                return this
            }
        }
    }
}
