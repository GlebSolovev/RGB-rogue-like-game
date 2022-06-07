package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuilder
import ru.hse.sd.rgb.gamelogic.engines.behaviour.NoneBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.AttackOnCollision
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.DieOnFatalAttack
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.DropItemOnDie
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.EnableColorUpdate
import ru.hse.sd.rgb.gamelogic.engines.fight.AttackType
import ru.hse.sd.rgb.gamelogic.engines.fight.ControlParams
import ru.hse.sd.rgb.gamelogic.engines.fight.HealType
import ru.hse.sd.rgb.gamelogic.engines.items.scriptitems.ColorModificationEntity
import ru.hse.sd.rgb.gamelogic.entities.ColorCellHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.gamelogic.entities.HpGameUnit
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.CloneTick
import ru.hse.sd.rgb.utils.messaging.messages.EntityUpdated
import ru.hse.sd.rgb.utils.messaging.messages.RepaintTick
import ru.hse.sd.rgb.utils.structures.*
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class Glitch(
    cell: Cell,
    hp: Int,
    private val clonePeriodMillis: Long,
    private val teamId: Int
) : GameEntity(setOf(ColorCellHp(RGB(0, 0, 0), cell, hp))) {

    companion object {
        const val REPAINT_PERIOD_MILLIS = 100L
        const val TO_RGB_DELTA_SCALE_COEFFICIENT = 0.1
        const val ON_DIE_ITEM_DROP_PROBABILITY = 0.8
    }

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit) = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.SQUARE, outlineColor)
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

    private val glitchBaseBehaviour = GlitchBaseBehaviour()
    override val lifecycle = BehaviourBuilder.lifecycle(this, glitchBaseBehaviour).addBlocks {
        add { AttackOnCollision(entity, childBlock) }
        add { DieOnFatalAttack(entity, childBlock) }
        add { EnableColorUpdate(entity, childBlock, ControlParams(AttackType.HERO_TARGET, HealType.RANDOM_TARGET)) }
        add {
            DropItemOnDie(entity, childBlock, ON_DIE_ITEM_DROP_PROBABILITY) {
                ColorModificationEntity(
                    cell,
                    convertGlitchColorToRGBDelta(units.first().gameColor)
                )
            }
        }
    }.build()

    private fun convertGlitchColorToRGBDelta(rgb: RGB): RGBDelta {
        val (r, g, b) = rgb
        return RGBDelta(
            (r * TO_RGB_DELTA_SCALE_COEFFICIENT).toInt(),
            (g * TO_RGB_DELTA_SCALE_COEFFICIENT).toInt(),
            (b * TO_RGB_DELTA_SCALE_COEFFICIENT).toInt()
        )
    }

    override val behaviourEntity = object : BehaviourEntity() {
        override fun createDirectAttackHeroBehaviour(baseBehaviour: Behaviour, movePeriodMillis: Long): Behaviour =
            glitchBaseBehaviour

        override fun createDirectFleeFromHeroBehaviour(baseBehaviour: Behaviour, movePeriodMillis: Long): Behaviour =
            glitchBaseBehaviour

        override fun createUponSeeingBehaviour(
            childBehaviour: Behaviour,
            targetEntity: GameEntity,
            seeingDepth: Int,
            createSeeingBehaviour: (Behaviour) -> Behaviour
        ): Behaviour = glitchBaseBehaviour
    }

    private inner class GlitchBaseBehaviour : NoneBehaviour(this) {
        private val repaintTicker = Ticker(REPAINT_PERIOD_MILLIS, this@Glitch, RepaintTick())
        private val cloneTicker = Ticker(clonePeriodMillis, this@Glitch, CloneTick())

        override fun traverseTickers(onEach: (Ticker) -> Unit) {
            onEach(repaintTicker)
            onEach(cloneTicker)
        }

        override suspend fun handleMessage(message: Message) {
            when (message) {
                is RepaintTick -> {
                    units.forEach { unit -> controller.fighting.changeRGB(unit, generateRandomColor()) }
                    controller.view.receive(EntityUpdated(this@Glitch))
                }
                is CloneTick -> {
                    val cells = units.map { it.cell }.toSet()
                    val adjacentCells =
                        cells.flatMap { cell -> Direction.values().map { cell + it.toShift() } }.toSet() subtract cells
                    val targetCell = adjacentCells.randomElement()

                    val clone = clone(targetCell)
                    val cloneIsPopulated = controller.creation.tryAddToWorld(clone)
                    if (cloneIsPopulated) controller.view.receive(EntityUpdated(clone))
                }
            }
        }
    }

    private fun clone(targetCell: Cell): Glitch =
        Glitch(targetCell, (units.first() as HpGameUnit).hp, clonePeriodMillis, teamId)
}
