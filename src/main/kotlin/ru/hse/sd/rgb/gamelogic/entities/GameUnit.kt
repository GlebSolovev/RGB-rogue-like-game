package ru.hse.sd.rgb.gamelogic.entities

import ru.hse.sd.rgb.utils.getValue
import ru.hse.sd.rgb.utils.setValue
import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.utils.structures.Direction
import ru.hse.sd.rgb.utils.structures.RGB
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

typealias GameUnitId = Long

sealed class GameUnit(
    val parent: GameEntity,
    gameColor: RGB,
    cell: Cell,
    lastMoveDir: Direction = Direction.random()
) {

    var gameColor: RGB by AtomicReference(gameColor)
    var cell: Cell by AtomicReference(cell)
    var lastMoveDir: Direction by AtomicReference(lastMoveDir)

    companion object {
        private val globalId = AtomicLong(0)
    }

    val id: GameUnitId = globalId.incrementAndGet()

    override fun equals(other: Any?) = this === other || id == (other as? GameUnit)?.id

    override fun hashCode(): Int = id.hashCode()
}

class NoHpGameUnit(
    parent: GameEntity,
    gameColor: RGB,
    cell: Cell
) : GameUnit(parent, gameColor, cell) {

    constructor(parent: GameEntity, colorCellNoHp: ColorCellNoHp) : this(
        parent,
        colorCellNoHp.color,
        colorCellNoHp.cell
    )
}

class HpGameUnit(
    parent: GameEntity,
    gameColor: RGB,
    cell: Cell,
    hp: Int,
    maxHp: Int,
) : GameUnit(parent, gameColor, cell) {

    var hp: Int by AtomicReference(hp)
    var maxHp: Int by AtomicReference(maxHp)

    constructor(parent: GameEntity, colorCellHp: ColorCellHp) : this(
        parent,
        colorCellHp.color,
        colorCellHp.cell,
        colorCellHp.hp,
        colorCellHp.maxHp
    )
}

@Serializable
sealed class ColorCell {
    abstract val color: RGB
    abstract val cell: Cell
}

@Serializable
data class ColorCellNoHp(override val color: RGB, override val cell: Cell) : ColorCell()

@Serializable
class ColorCellHp(override val color: RGB, override val cell: Cell, val hp: Int, val maxHp: Int) : ColorCell() {
    constructor(color: RGB, cell: Cell, maxHp: Int) : this(color, cell, maxHp, maxHp)
}
