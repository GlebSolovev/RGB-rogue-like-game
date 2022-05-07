package ru.hse.sd.rgb.gamelogic.entities

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.utils.RGB
import ru.hse.sd.rgb.utils.Tick
import ru.hse.sd.rgb.utils.Ticker
import java.util.concurrent.atomic.AtomicLong

typealias GameUnitId = Long

class GameUnit(
    val parent: GameEntity,
    var cell: Cell,
    var hp: Int,
    var gameColor: RGB,
) {
    // WARNING!!! Do not shuffle initialization order!

    companion object {
        private val globalId = AtomicLong(0)
    }

    val id: GameUnitId = globalId.incrementAndGet()

    private val colorTicker = Ticker(
        controller.fighting.getBaseColorStats(this).updatePeriodMillis,
        parent,
        ColorTick(this)
    ).also {
        it.start()
    } // TODO: update when base color changes

    override fun equals(other: Any?) = this === other || id == (other as? GameUnit)?.id

    override fun hashCode(): Int = id.hashCode()
}

data class ColorHpCell(val color: RGB, val hp: Int, val cell: Cell)

data class ColorTick(val unit: GameUnit) : Tick()
