package ru.hse.sd.rgb.gameloaders.generators

import ru.hse.sd.rgb.utils.Cell
import kotlin.random.Random
import kotlin.random.nextInt

fun generateMaze(
    widthRange: IntRange,
    heightRange: IntRange,
    minPathSizeRange: IntRange,
    passageSizeRange: IntRange,
    random: Random = Random
): List<List<Boolean>> {
    val w = random.nextInt(widthRange)
    val h = random.nextInt(heightRange)
    val minPathSize = random.nextInt(minPathSizeRange)
    val passageSize = random.nextInt(passageSizeRange)
    return generateMaze(w, h, minPathSize, passageSize, random)
}

// recursive division method
// might have inaccessible areas, prob of that ~= 0 when minPathSize >= 3
fun generateMaze(
    w: Int,
    h: Int,
    minPathSize: Int,
    passageSize: Int,
    random: Random = Random
): List<List<Boolean>> {
    val grid = List(h) { MutableList(w) { false } }
    splitChamber(Chamber(Cell(0, 0), Cell(w - 1, h - 1)), grid, minPathSize, passageSize, random)
    return grid
}

private data class Chamber(val lb: Cell, val ru: Cell) {
    init {
        assert(lb.x <= ru.x && lb.y <= ru.y)
    }
}

private fun splitChamber(
    c: Chamber,
    grid: List<MutableList<Boolean>>,
    minSize: Int,
    passageSize: Int,
    random: Random
) {

    fun trySplitHorizontally(): Pair<Chamber, Chamber>? {
        val dy = c.ru.y - c.lb.y + 1
        val variance = dy - minSize * 2
        if (variance <= 0) return null // TODO: fight duplicated code fragment
        val ry = random.nextInt(variance)
        val yWall = c.lb.y + minSize + ry

        val dx = c.ru.x - c.lb.x + 1
        val xHole = if (dx <= passageSize) {
            c.lb.x..c.ru.x
        } else {
            val xStart = random.nextInt(c.lb.x, c.ru.x - passageSize + 1)
            xStart until xStart + passageSize
        }

        for (x in c.lb.x..c.ru.x) grid[yWall][x] = true
        for (x in xHole) grid[yWall][x] = false

        return Pair(
            Chamber(c.lb, Cell(c.ru.x, yWall - 1)),
            Chamber(Cell(c.lb.x, yWall + 1), c.ru)
        )
    }

    fun trySplitVertically(): Pair<Chamber, Chamber>? {
        val dx = c.ru.x - c.lb.x
        val variance = dx - minSize * 2 + 1
        if (variance <= 0) return null
        val rx = random.nextInt(variance)
        val xHole = c.lb.x + minSize + rx

        val dy = c.ru.y - c.lb.y + 1
        val yHole = if (dy <= passageSize) {
            c.lb.y..c.ru.y
        } else {
            val yStart = random.nextInt(c.lb.y, c.ru.y - passageSize + 1)
            yStart until yStart + passageSize
        }

        for (y in c.lb.y..c.ru.y) grid[y][xHole] = true
        for (y in yHole) grid[y][xHole] = false

        return Pair(
            Chamber(c.lb, Cell(xHole - 1, c.ru.y)),
            Chamber(Cell(xHole + 1, c.lb.y), c.ru)
        )
    }

    val isVertical = random.nextBoolean()
    val result = if (isVertical) trySplitVertically() else trySplitHorizontally()
    result?.let { (c1, c2) ->
        splitChamber(c1, grid, minSize, passageSize, random)
        splitChamber(c2, grid, minSize, passageSize, random)
    }
}
