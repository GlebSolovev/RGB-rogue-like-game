package ru.hse.sd.rgb.gamelogic.entities

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.Lifecycle
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta.*
import ru.hse.sd.rgb.utils.messaging.Messagable
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.messages.EntityUpdated
import ru.hse.sd.rgb.utils.messaging.messages.SetEffectColor
import ru.hse.sd.rgb.utils.structures.Direction
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.views.GameEntityViewSnapshot
import ru.hse.sd.rgb.views.ViewUnit
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class GameEntity(colorCells: Set<ColorCell>) : Messagable() {

    abstract inner class ViewEntity {
        protected abstract fun convertUnit(unit: GameUnit): ViewUnit

        fun takeViewSnapshot(): GameEntityViewSnapshot {
            return units.map { convertUnit(it) }.toSet()
        }

        protected var outlineColor: RGB? = null

        open fun applyMessageToAppearance(m: Message) {
            // TODO: remember stack of effects to unset them correctly
            if (m is SetEffectColor) {
                outlineColor = if (m.enabled) m.color else null
                controller.view.receive(EntityUpdated(this@GameEntity))
            }
        }
    }

    abstract inner class PhysicalEntity {
        abstract val isSolid: Boolean // if true, only this entity can occupy its cells
        // same as 'no one can pass through this entity'

        abstract fun getUnitDirection(unit: GameUnit, dir: Direction): Direction

        open fun filterIncompatibleUnits(
            physicalEntity: GameEntity.PhysicalEntity,
            units: Set<GameUnit>
        ): Set<GameUnit> {
            return if ((physicalEntity.isSolid && units.isNotEmpty()) ||
                units.any { it.parent.physicalEntity.isSolid }
            ) units else emptySet()
        }
    }

    abstract inner class FightEntity {
        abstract fun isUnitActive(unit: GameUnit): Boolean
        abstract val teamId: Int
    }

    open inner class BehaviourEntity {
        open fun createDirectAttackHeroBehaviour(
            baseBehaviour: Behaviour,
            movePeriodMillis: Long,
        ): Behaviour = DirectAttackHeroBehaviour(baseBehaviour, movePeriodMillis)

        open fun createDirectFleeFromHeroBehaviour(
            baseBehaviour: Behaviour,
            movePeriodMillis: Long,
        ): Behaviour = DirectFleeFromHeroBehaviour(baseBehaviour, movePeriodMillis)

        open fun createUponSeeingBehaviour(
            childBehaviour: Behaviour,
            targetEntity: GameEntity,
            seeingDepth: Int,
            createSeeingBehaviour: (Behaviour) -> Behaviour
        ): Behaviour =
            UponSeeingBehaviour(this@GameEntity, childBehaviour, targetEntity, seeingDepth, createSeeingBehaviour)

        open fun createConfusedBehaviour(
            childBehaviour: Behaviour
        ): Behaviour = ConfusedBehaviour(this@GameEntity, childBehaviour)

        open fun createBurningBehaviour(
            childBehaviour: Behaviour,
            attackPeriodMillis: Long,
            attack: Int,
            initialDurationMillis: Long?
        ): Behaviour = BurningBehaviour(
            this@GameEntity,
            childBehaviour,
            attackPeriodMillis,
            attack,
            initialDurationMillis
        )

        open fun createFrozenBehaviour(
            childBehaviour: Behaviour,
            slowDownCoefficient: Double
        ): Behaviour = FrozenBehaviour(this@GameEntity, childBehaviour, slowDownCoefficient)
    }

    open inner class SingleBehaviourEntity(private val singleBehaviour: Behaviour) : BehaviourEntity() {
        override fun createDirectAttackHeroBehaviour(baseBehaviour: Behaviour, movePeriodMillis: Long) =
            singleBehaviour

        override fun createDirectFleeFromHeroBehaviour(baseBehaviour: Behaviour, movePeriodMillis: Long) =
            singleBehaviour

        override fun createUponSeeingBehaviour(
            childBehaviour: Behaviour,
            targetEntity: GameEntity,
            seeingDepth: Int,
            createSeeingBehaviour: (Behaviour) -> Behaviour
        ): Behaviour = singleBehaviour

        override fun createConfusedBehaviour(childBehaviour: Behaviour): Behaviour = singleBehaviour

        override fun createBurningBehaviour(
            childBehaviour: Behaviour,
            attackPeriodMillis: Long,
            attack: Int,
            initialDurationMillis: Long?
        ): Behaviour = singleBehaviour

        override fun createFrozenBehaviour(
            childBehaviour: Behaviour,
            slowDownCoefficient: Double
        ): Behaviour = singleBehaviour
    }

    abstract val viewEntity: ViewEntity
    abstract val physicalEntity: PhysicalEntity
    abstract val fightEntity: FightEntity
    abstract val behaviourEntity: BehaviourEntity

    val units: MutableSet<GameUnit> = Collections.newSetFromMap(ConcurrentHashMap())
//     TODO: maybe make private and not concurrent

    init {
        units.addAll(
            colorCells.map { cell ->
                when (cell) {
                    is ColorCellHp -> HpGameUnit(this, cell)
                    is ColorCellNoHp -> NoHpGameUnit(this, cell)
                }
            }
        )
    }

    open fun onLifeStart() {}
    open fun onLifeEnd() {}

    abstract val lifecycle: Lifecycle

    override suspend fun handleMessage(m: Message) = lifecycle.handleMessage(m)
}
