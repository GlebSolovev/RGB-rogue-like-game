package ru.hse.sd.rgb.views.swing

data class SwingUnitAppearance(val shape: SwingUnitShape) // TODO: outline

enum class SwingUnitShape {
    SQUARE,
    CIRCLE,
    TRIANGLE,
}
