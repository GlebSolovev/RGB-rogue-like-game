package ru.hse.sd.rgb.views

import java.awt.Color

data class SwingEntityAppearance(val shape: SwingUnitShape, val color: Color)

enum class SwingUnitShape {
    SQUARE,
    CIRCLE,
    TRIANGLE,
}
