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

private data class Chamber(val leftBottom: Cell, val rightTop: Cell) {
    init {
        assert(leftBottom.x <= rightTop.x && leftBottom.y <= rightTop.y)
    }
}

private fun splitChamber(
    chamber: Chamber,
    grid: Grid2D<Boolean>,
    minSize: Int,
    passageSize: Int,
    random: Random
) {

    fun trySplitHorizontally(): Pair<Chamber, Chamber>? {
        val dy = chamber.rightTop.y - chamber.leftBottom.y + 1
        val variance = dy - minSize * 2
        if (variance <= 0) return null // TODO: fight duplicated code fragment
        val ry = random.nextInt(variance)
        val yWall = chamber.leftBottom.y + minSize + ry

        val dx = chamber.rightTop.x - chamber.leftBottom.x + 1
        val xHole = if (dx <= passageSize) {
            chamber.leftBottom.x..chamber.rightTop.x
        } else {
            val xStart = random.nextInt(chamber.leftBottom.x, chamber.rightTop.x - passageSize + 1)
            xStart until xStart + passageSize
        }

        for (x in chamber.leftBottom.x..chamber.rightTop.x) {
            grid[x, yWall] = true
        }
        for (x in xHole) {
            grid[x, yWall] = false
        }

        return Pair(
            Chamber(chamber.leftBottom, Cell(chamber.rightTop.x, yWall - 1)),
            Chamber(Cell(chamber.leftBottom.x, yWall + 1), chamber.rightTop)
        )
    }

    fun trySplitVertically(): Pair<Chamber, Chamber>? {
        val dx = chamber.rightTop.x - chamber.leftBottom.x
        val variance = dx - minSize * 2 + 1
        if (variance <= 0) return null
        val rx = random.nextInt(variance)
        val xHole = chamber.leftBottom.x + minSize + rx

        val dy = chamber.rightTop.y - chamber.leftBottom.y + 1
        val yHole = if (dy <= passageSize) {
            chamber.leftBottom.y..chamber.rightTop.y
        } else {
            val yStart = random.nextInt(chamber.leftBottom.y, chamber.rightTop.y - passageSize + 1)
            yStart until yStart + passageSize
        }

        for (y in chamber.leftBottom.y..chamber.rightTop.y) grid[xHole, y] = true
        for (y in yHole) grid[xHole, y] = false

        return Pair(
            Chamber(chamber.leftBottom, Cell(xHole - 1, chamber.rightTop.y)),
            Chamber(Cell(xHole + 1, chamber.leftBottom.y), chamber.rightTop)
        )
    }

    val isVertical = random.nextBoolean()
    val result = if (isVertical) trySplitVertically() else trySplitHorizontally()
    result?.let { (c1, c2) ->
        splitChamber(c1, grid, minSize, passageSize, random)
        splitChamber(c2, grid, minSize, passageSize, random)
    }
}
