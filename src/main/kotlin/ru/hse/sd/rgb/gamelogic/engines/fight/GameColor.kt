package ru.hse.sd.rgb.gamelogic.engines.fight

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.utils.RGB

data class GameColor(var rgb: RGB) {

    var cachedBaseColorId = controller.fighting.resolveBaseColor(rgb)

}
