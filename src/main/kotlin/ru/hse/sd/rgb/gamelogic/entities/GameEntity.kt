package ru.hse.sd.rgb.gamelogic.entities

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.utils.messaging.*
import ru.hse.sd.rgb.utils.messaging.messages.EntityRemoved
import ru.hse.sd.rgb.utils.messaging.messages.EntityUpdated
import ru.hse.sd.rgb.utils.messaging.messages.LifeEnded
import ru.hse.sd.rgb.utils.messaging.messages.LifeStarted
import ru.hse.sd.rgb.views.GameEntityViewSnapshot
import ru.hse.sd.rgb.views.ViewUnit
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

abstract class GameEntity(colorCells: Set<ColorCell>) : Messagable() {

    abstract inner class ViewEntity {
        protected abstract fun convertUnit(unit: GameUnit): ViewUnit

        fun takeViewSnapshot(): GameEntityViewSnapshot {
            return units.map { convertUnit(it) }.toSet()
        }

        open fun applyMessageToAppearance(m: Message) {}
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

    abstract val viewEntity: ViewEntity
    abstract val physicalEntity: PhysicalEntity
    abstract val fightEntity: FightEntity

    val units: MutableSet<GameUnit> = Collections.newSetFromMap(ConcurrentHashMap())
    // TODO: maybe make private and not concurrent

    init {
        units.addAll(colorCells.map { cell ->
            when (cell) {
                is ColorCellHp -> HpGameUnit(this, cell)
                is ColorCellNoHp -> NoHpGameUnit(this, cell)
            }
        })
    }

    var lifeCycleState: EntityLifeCycleState by AtomicReference(EntityLifeCycleState.NOT_STARTED)

    final override suspend fun handleMessage(m: Message) {
        when (lifeCycleState) {
            EntityLifeCycleState.NOT_STARTED -> {
                when (m) {
                    is LifeStarted -> {
                        lifeCycleState = EntityLifeCycleState.ONGOING
                        controller.view.receive(EntityUpdated(this))
                        onLifeStart()
                    }
                    is LifeEnded -> lifeCycleState = EntityLifeCycleState.DEAD
                    else -> ignore
                }
            }
            EntityLifeCycleState.ONGOING -> {
                when (m) {
                    is LifeStarted -> unreachable
                    is LifeEnded -> {
                        lifeCycleState = EntityLifeCycleState.DEAD
                        m.dieRoutine()
                        controller.view.receive(EntityRemoved(this))
                        onLifeEnd()
                    }
                    else -> {
                        handleGameMessage(m)
                        viewEntity.applyMessageToAppearance(m)
                    }
                }
            }
            EntityLifeCycleState.DEAD -> {
                when (m) {
                    is LifeStarted -> unreachable
                    else -> ignore
                }
            }
        }
    }

    open fun onLifeStart() {}
    open fun onLifeEnd() {}

    abstract suspend fun handleGameMessage(m: Message)
}

enum class EntityLifeCycleState { NOT_STARTED, ONGOING, DEAD }