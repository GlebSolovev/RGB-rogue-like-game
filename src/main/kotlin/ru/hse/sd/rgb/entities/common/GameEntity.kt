package ru.hse.sd.rgb.entities.common

import ru.hse.sd.rgb.*
import ru.hse.sd.rgb.views.GameEntityViewSnapshot
import ru.hse.sd.rgb.views.ViewUnit

abstract class GameEntity(colorCells: Set<ColorCell>) : Messagable() {

    abstract inner class ViewEntity {
        protected abstract fun convertUnit(unit: GameUnit): ViewUnit

        fun takeViewSnapshot(): GameEntityViewSnapshot {
            return units.map { convertUnit(it) }.toSet()
        }

        open fun applyMessageToAppearance(m: Message) {}
    }

    abstract inner class PhysicalEntity {
        abstract val isSolid: Boolean // if true, only this entity can occupy its cells TODO: physics
    }

    abstract val physicalEntity: PhysicalEntity
    abstract val viewEntity: ViewEntity

    val units: Set<GameUnit>

    init {
        val cells = colorCells.map { it.cell }.toSet()
        val shifts = setOf(GridShift(-1, 0), GridShift(1, 0), GridShift(0, 1), GridShift(0, -1))
        units = mutableSetOf()
        outer@ for ((cell, color) in colorCells) {
            for (shift in shifts) {
                if (cell + shift !in cells) {
                    units.add(GameUnit(this, cell, color, true))
                    continue@outer
                }
            }
            units.add(GameUnit(this, cell, color, false))
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
