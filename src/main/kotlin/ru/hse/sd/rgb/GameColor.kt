package ru.hse.sd.rgb

import ru.hse.sd.rgb.views.RGB

abstract class GameColor(var rgb: RGB) {

    var cachedBaseColorId = controller.fighting.resolveBaseColor(rgb)

}
