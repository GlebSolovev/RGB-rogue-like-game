package ru.hse.sd.rgb.gameloaders

import ru.hse.sd.rgb.gamelogic.engines.fight.BaseColorId
import ru.hse.sd.rgb.gamelogic.engines.fight.BaseColorStats

data class BaseColorsDescription(
    val baseColorStats: Map<BaseColorId, BaseColorStats>,
    val attackFromTo: Map<Pair<BaseColorId, BaseColorId>, Int>
)

fun loadColors(filename: String): BaseColorsDescription {
    TODO()
}