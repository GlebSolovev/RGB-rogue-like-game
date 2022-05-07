package ru.hse.sd.rgb.gamelogic.entities

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.utils.RGB
import ru.hse.sd.rgb.utils.Tick
import ru.hse.sd.rgb.utils.Ticker
import java.util.concurrent.atomic.AtomicLong

typealias GameUnitId = Long

sealed class GameUnit(
    val parent: GameEntity,
    var gameColor: RGB,
    var cell: Cell,
) {
    // WARNING!!! Do not shuffle initialization order!

    companion object {
        private val globalId = AtomicLong(0)
    }

    val id: GameUnitId = globalId.incrementAndGet()

    private val colorTicker = Ticker(
        controller.fighting.getBaseColorStats(this).updatePeriodMillis,
        parent,
        ColorTick(this) // is ok
    ).also {
        it.start()
    } // TODO: update when base color changes

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
    var hp: Int,
    val maxHp: Int,
) : GameUnit(parent, gameColor, cell) {

    constructor(parent: GameEntity, colorCellHp: ColorCellHp) : this(
        parent,
        colorCellHp.color,
        colorCellHp.cell,
        colorCellHp.maxHp,
        colorCellHp.maxHp
    )

}

sealed class ColorCell(open val color: RGB, open val cell: Cell)

data class ColorCellNoHp(override val color: RGB, override val cell: Cell) : ColorCell(color, cell)

class ColorCellHp(override val color: RGB, override val cell: Cell, val maxHp: Int) :
    ColorCell(color, cell)

data class ColorTick(val unit: GameUnit) : Tick()
