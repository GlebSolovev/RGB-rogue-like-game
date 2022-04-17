package ru.hse.sd.rgb

import ru.hse.sd.rgb.views.RGB

class GameColor(rgb: RGB) {

    constructor(r: Int, g: Int, b: Int) : this(RGB(r, g, b))

    var rgb: RGB = rgb
        private set

}
