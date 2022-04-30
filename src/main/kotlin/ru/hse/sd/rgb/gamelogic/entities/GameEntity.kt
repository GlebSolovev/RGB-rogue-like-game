package ru.hse.sd.rgb.gamelogic.entities

import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.views.GameEntityViewSnapshot
import ru.hse.sd.rgb.views.ViewUnit
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

abstract class GameEntity(colorHpCells: Set<ColorHpCell>) : Messagable() {

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
    }

    abstract inner class FightEntity { // TODO: for fight logic and behaviour state machines, effects
        abstract fun isUnitActive(unit: GameUnit): Boolean
        // TODO: add isImmortal flag (and check it in FightLogic)
    }

    abstract val viewEntity: ViewEntity
    abstract val physicalEntity: PhysicalEntity

    val units: MutableSet<GameUnit> = Collections.newSetFromMap(ConcurrentHashMap())

    init {
        val cells = colorHpCells.map { it.cell }.toSet()
        val shifts = setOf(GridShift(-1, 0), GridShift(1, 0), GridShift(0, 1), GridShift(0, -1))
        outer@ for ((color, hp, cell) in colorHpCells) {
            for (shift in shifts) {
                if (cell + shift !in cells) {
                    units.add(GameUnit(this, cell, hp, color))
                    continue@outer
                }
            }
            units.add(GameUnit(this, cell, hp, color))
        }
    }

    private var gameStarted = false

    final override suspend fun handleMessage(m: Message) {
        if (gameStarted) {
            handleGameMessage(m)
        } else {
            gameStarted = m is GameStarted
            onGameStart()
        }
    }

    open fun onGameStart() {}

    abstract suspend fun handleGameMessage(m: Message)

}

class GameStarted : Message()
data class CollidedWith(val myUnit: GameUnit, val otherUnit: GameUnit) : Message()
data class ReceivedAttack(val myUnit: GameUnit, val fromUnit: GameUnit, val isFatal: Boolean) : Message()

class MoveTick : Tick()