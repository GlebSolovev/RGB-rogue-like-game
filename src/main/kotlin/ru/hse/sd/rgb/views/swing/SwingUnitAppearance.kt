package ru.hse.sd.rgb.views.swing

import ru.hse.sd.rgb.utils.Direction

data class SwingUnitAppearance(
    val shape: SwingUnitShape,
    val scale: Double = 1.0,
) // TODO: outline (for effects!)

enum class SwingUnitShape {
    SQUARE,
    CIRCLE,
    TRIANGLE_UP,
    TRIANGLE_LEFT,
    TRIANGLE_DOWN,
    TRIANGLE_RIGHT,
    CIRCLE_HALF_UP,
    CIRCLE_HALF_LEFT,
    CIRCLE_HALF_DOWN,
    CIRCLE_HALF_RIGHT,
    RECTANGLE_HORIZONTAL,
    RECTANGLE_VERTICAL,
    STAR_8,
    PLUS,
    SPINNING_SQUARE,
    SPIRAL,
    ;

    companion object {
        fun TRIANGLE(dir: Direction) = when(dir) {
            Direction.UP, Direction.NOPE -> TRIANGLE_UP
            Direction.LEFT -> TRIANGLE_LEFT
            Direction.DOWN -> TRIANGLE_DOWN
            Direction.RIGHT -> TRIANGLE_RIGHT
        }
        fun CIRCLE_HALF(dir: Direction) = when(dir) {
            Direction.UP, Direction.NOPE -> CIRCLE_HALF_UP
            Direction.LEFT -> CIRCLE_HALF_LEFT
            Direction.DOWN -> CIRCLE_HALF_DOWN
            Direction.RIGHT -> CIRCLE_HALF_RIGHT
        }
        fun RECTANGLE(dir: Direction) = when(dir) {
            Direction.UP, Direction.DOWN, Direction.NOPE -> RECTANGLE_VERTICAL
            Direction.LEFT, Direction.RIGHT -> RECTANGLE_HORIZONTAL
        }
    }
}


