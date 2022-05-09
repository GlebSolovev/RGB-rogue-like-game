package ru.hse.sd.rgb.views.swing

import ru.hse.sd.rgb.utils.Direction

data class SwingUnitAppearance(
    val shape: SwingUnitShape,
    val scale: Double = 1.0,
) // TODO: outline

enum class SwingUnitShape {
    SQUARE,
    CIRCLE,
    TRIANGLE_UP,
    TRIANGLE_LEFT,
    TRIANGLE_DOWN,
    TRIANGLE_RIGHT;

    companion object {
        fun TRIANGLE(dir: Direction) = when(dir) {
            Direction.UP, Direction.NOPE -> TRIANGLE_UP
            Direction.LEFT -> TRIANGLE_LEFT
            Direction.DOWN -> TRIANGLE_DOWN
            Direction.RIGHT -> TRIANGLE_RIGHT
        }
    }
}


