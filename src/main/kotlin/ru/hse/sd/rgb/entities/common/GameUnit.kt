package ru.hse.sd.rgb.entities.common

import ru.hse.sd.rgb.Cell
import ru.hse.sd.rgb.GameColor

data class GameUnit(
    val parent: GameEntity,
    var cell: Cell,
    val gameColor: GameColor,
    var isBoundary: Boolean,
) {

    override fun equals(other: Any?): Boolean {
        return if (other is GameUnit) cell == other.cell else false
    }

    override fun hashCode(): Int {
        return cell.hashCode()
    }

}

data class ColorCell(val cell: Cell, val color: GameColor)
