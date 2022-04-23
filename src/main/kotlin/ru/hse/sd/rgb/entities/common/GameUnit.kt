package ru.hse.sd.rgb.entities.common

import ru.hse.sd.rgb.*
import java.util.concurrent.atomic.AtomicLong

typealias GameUnitId = Long

data class GameUnit(
    val parent: GameEntity,
    var cell: Cell,
    var hp: Int,
    val gameColor: GameColor,
) {

    private val colorTicker = Ticker(
        controller.fighting.getBaseColorStats(gameColor.cachedBaseColorId).updatePeriodMillis,
        parent,
        ColorTick(this)
    ).also { it.start() }

    companion object {
        private val globalId = AtomicLong(0)
    }

    val id: GameUnitId = globalId.incrementAndGet()

    override fun equals(other: Any?) = this === other || id == (other as? GameUnit)?.id

    override fun hashCode(): Int = id.hashCode()

}

data class ColorCell(val color: GameColor, val cell: Cell)

data class ColorTick(val unit: GameUnit) : Tick()
