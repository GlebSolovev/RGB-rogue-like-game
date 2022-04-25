package ru.hse.sd.rgb.gamelogic.entities

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.engines.fight.GameColor
import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.utils.Tick
import ru.hse.sd.rgb.utils.Ticker
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
    ).also { it.start() } // TODO: update when base color changes

    companion object {
        private val globalId = AtomicLong(0)
    }

    val id: GameUnitId = globalId.incrementAndGet()

    override fun equals(other: Any?) = this === other || id == (other as? GameUnit)?.id

    override fun hashCode(): Int = id.hashCode()

}

data class ColorHpCell(val color: GameColor, val hp: Int, val cell: Cell)

data class ColorTick(val unit: GameUnit) : Tick()
