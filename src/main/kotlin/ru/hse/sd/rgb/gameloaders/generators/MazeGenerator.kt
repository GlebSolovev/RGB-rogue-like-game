package ru.hse.sd.rgb.gameloaders.generators

import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.utils.structures.Grid2D
import kotlin.random.Random
import kotlin.random.nextInt

fun generateMaze(
    w: Int,
    h: Int,
    minPathSizeRange: IntRange,
    passageSizeRange: IntRange,
    random: Random = Random
): Grid2D<Boolean> {
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
): Grid2D<Boolean> {
    val grid = Grid2D(w, h) { _, _ -> false }
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
    grid: Grid2D<Boolean>,
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

        for (x in c.lb.x..c.ru.x) grid[x, yWall] = true
        for (x in xHole) grid[x, yWall] = false

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

        for (y in c.lb.y..c.ru.y) grid[xHole, y] = true
        for (y in yHole) grid[xHole, y] = false

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
