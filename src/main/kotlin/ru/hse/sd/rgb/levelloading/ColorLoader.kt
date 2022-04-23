package ru.hse.sd.rgb.levelloading

import ru.hse.sd.rgb.logic.BaseColorId
import ru.hse.sd.rgb.logic.BaseColorStats
import ru.hse.sd.rgb.views.RGB

data class BaseColorsDescription(
    val baseColorStats: Map<BaseColorId, BaseColorStats>,
    val attackFromTo: Map<Pair<BaseColorId, BaseColorId>, Int>
)

data class

fun loadColors(filename: String): BaseColorsDescription {

}