package ru.hse.sd.rgb.utils.structures

import kotlin.random.Random

data class Cell(val x: Int, val y: Int)
data class GridShift(val dx: Int, val dy: Int)

operator fun Cell.plus(p: GridShift) = Cell(x + p.dx, y + p.dy)

enum class Direction {
    UP, LEFT, DOWN, RIGHT, NOPE;

    // (0, 0) is upper-left corner
    fun toShift(): GridShift = when (this) {
        UP -> GridShift(0, -1)
        LEFT -> GridShift(-1, 0)
        DOWN -> GridShift(0, 1)
        RIGHT -> GridShift(1, 0)
        NOPE -> GridShift(0, 0)
    }

    fun opposite(): Direction = when (this) {
        UP -> DOWN
        LEFT -> RIGHT
        DOWN -> UP
        RIGHT -> LEFT
        NOPE -> NOPE
    }

    val isVertical
        get() = this == UP || this == DOWN
    val isHorizontal
        get() = this == LEFT || this == RIGHT

    companion object {
        fun random(random: Random = Random) = listOf(UP, LEFT, DOWN, RIGHT).random(random)
    }
}
