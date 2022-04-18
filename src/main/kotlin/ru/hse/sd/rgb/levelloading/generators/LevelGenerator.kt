package ru.hse.sd.rgb.levelloading.generators

import ru.hse.sd.rgb.Cell
import ru.hse.sd.rgb.GameColor
import ru.hse.sd.rgb.entities.Hero
import ru.hse.sd.rgb.entities.Wall
import ru.hse.sd.rgb.entities.common.ColorCell
import ru.hse.sd.rgb.entities.common.GameEntity
import ru.hse.sd.rgb.levelloading.LevelDescription
import ru.hse.sd.rgb.views.RGB
import ru.hse.sd.rgb.get
import kotlin.random.Random
import kotlin.random.nextInt

private object RANDOM_RANGES {
    val WIDTH_RANGE = 70..90
    val HEIGHT_RANGE = 25..40
    val MINSIZE_RANGE = 4..7
    val PASSAGE_RANGE = 3..5
    val COLOR_RANGE = 0..50
}

private const val MAX_HERO_CELL_RANDOM_ATTEMPTS = 1000
private val DEFAULT_HERO_COLOR = GameColor(40, 40, 40)
private val DEFAULT_WALL_COLOR = GameColor(255, 255, 255)

fun generateLevel(): LevelDescription {
    val random = Random
    val maze = generateMaze(
        RANDOM_RANGES.WIDTH_RANGE,
        RANDOM_RANGES.HEIGHT_RANGE,
        RANDOM_RANGES.MINSIZE_RANGE,
        RANDOM_RANGES.PASSAGE_RANGE,
        random
    )
    val h = maze.size
    val w = maze[0].size

    val (r, g, b) = List(3) { random.nextInt(RANDOM_RANGES.COLOR_RANGE) }
    val bgColor = RGB(r, g, b)

    val entities = mutableSetOf<GameEntity>()
    for (x in 0 until w) for (y in 0 until h) if (maze[y][x]) entities.add(Wall(Cell(x, y), DEFAULT_WALL_COLOR))

    var heroCell: Cell? = null
    repeat(MAX_HERO_CELL_RANDOM_ATTEMPTS) {
        heroCell = Cell(random.nextInt(w), random.nextInt(h))
        if (!maze[heroCell!!])
            return@repeat
        heroCell = null
    }
    if (heroCell == null) throw IllegalStateException("too many attempts")
    entities.add(Hero(setOf(ColorCell(heroCell!!, DEFAULT_HERO_COLOR))))

    return LevelDescription(w, h, entities, bgColor)
}