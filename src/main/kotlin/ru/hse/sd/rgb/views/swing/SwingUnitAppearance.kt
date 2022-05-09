package ru.hse.sd.rgb.views.swing

data class SwingUnitAppearance(
    val shape: SwingUnitShape,
    val scale: Double = 1.0,
) // TODO: outline

enum class SwingUnitShape {
    SQUARE,
    CIRCLE,
    TRIANGLE,
}
