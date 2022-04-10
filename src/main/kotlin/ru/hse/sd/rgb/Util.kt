package ru.hse.sd.rgb

data class Cell(val x: Int, val y: Int)
data class GridShift(val dx: Int, val dy: Int)

operator fun Cell.plus(p: GridShift) = Cell(x + p.dx, y + p.dy)

enum class Direction {
    UP, LEFT, DOWN, RIGHT;

    // (0, 0) is upper-left corner
    fun toShift(): GridShift = when (this) {
        UP -> GridShift(0, -1)
        LEFT -> GridShift(-1, 0)
        DOWN -> GridShift(0, 1)
        RIGHT -> GridShift(1, 0)
    }
}

operator fun <T> List<List<T>>.get(cell: Cell) = this[cell.y][cell.x]

operator fun <T> List<MutableList<T>>.set(cell: Cell, value: T) {
    this[cell.y][cell.x] = value
}

val ignore = Unit
val unreachable: Nothing
    get() = throw RuntimeException("reached unreachable")
