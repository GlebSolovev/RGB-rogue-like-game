package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gamelogic.behaviours.Behaviour
import ru.hse.sd.rgb.gamelogic.behaviours.SimpleBehaviour
import ru.hse.sd.rgb.gamelogic.behaviours.State
import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.engines.fight.AttackType
import ru.hse.sd.rgb.gamelogic.engines.fight.ControlParams
import ru.hse.sd.rgb.gamelogic.engines.fight.HealType
import ru.hse.sd.rgb.gamelogic.entities.*
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.utils.Ticker.Companion.createTicker
import ru.hse.sd.rgb.utils.messaging.messages.*
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape
import kotlin.random.Random

class Glitch(
    cell: Cell,
    hp: Int,
    private val teamId: Int
) : GameEntity(setOf(ColorCellHp(RGB(0, 0, 0), cell, hp))) {

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit) = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.SQUARE)
        }
    }

    override val physicalEntity = object : PhysicalEntity() {
        override val isSolid = false
        override fun getUnitDirection(unit: GameUnit, dir: Direction): Direction = Direction.NOPE
        override fun filterIncompatibleUnits(physicalEntity: PhysicalEntity, units: Set<GameUnit>): Set<GameUnit> {
            return units.filter { it.parent is Glitch }.toSet()
        }
    }

    override val fightEntity = object : FightEntity() {
        override fun isUnitActive(unit: GameUnit): Boolean = true
        override val teamId = this@Glitch.teamId
    }

    private val random = Random

    override var behaviour: Behaviour = GlitchDefaultBehaviour()
    override val behaviourEntity = SingleBehaviourEntity(behaviour) // TODO: normal behaviourEntity

    private inner class GlitchDefaultBehaviour : SimpleBehaviour() {

        private val repaintTicker = createTicker(30, RepaintTick()).also { it.start() }
        private val moveTicker = createTicker(5000, MoveTick()).also { it.start() }
        // TODO: move constants to parameters

        override var state = object : State() {

            override suspend fun handleReceivedAttack(message: ReceivedAttack): State {
                if (message.isFatal) controller.creation.die(this@Glitch)
                return this
            }

            override suspend fun handleCollidedWith(message: CollidedWith): State {
                controller.fighting.attack(message.myUnit, message.otherUnit)
                return this
            }

            override suspend fun handleColorTick(tick: ColorTick): State {
                controller.fighting.update(tick.unit, ControlParams(AttackType.HERO_TARGET, HealType.RANDOM_TARGET))
                return this
            }

            override suspend fun handleMoveTick(): State {
                val cells = units.map { it.cell }.toSet()
                val adjacentCells =
                    cells.flatMap { cell -> Direction.values().map { cell + it.toShift() } }.toSet() subtract cells
                val targetCell = adjacentCells.randomElement(random)

                val clone = clone(targetCell)
                val cloneIsPopulated = controller.creation.tryAddToWorld(clone)
                if (cloneIsPopulated) controller.view.receive(EntityUpdated(clone))
                return this
            }

            override suspend fun handleRepaintTick(): State {
                units.forEach { unit -> controller.fighting.changeRGB(unit, generateRandomColor(random)) }
                controller.view.receive(EntityUpdated(this@Glitch))
                return this
            }
        }
    }

    private fun clone(targetCell: Cell): Glitch = Glitch(targetCell, (units.first() as HpGameUnit).hp, teamId)

}