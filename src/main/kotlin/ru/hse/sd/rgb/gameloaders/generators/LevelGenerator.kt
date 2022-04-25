package ru.hse.sd.rgb.gameloaders.generators

import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.gamelogic.engines.fight.GameColor
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Hero
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Wall
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gameloaders.LevelDescription
import ru.hse.sd.rgb.gamelogic.entities.ColorHpCell
import ru.hse.sd.rgb.utils.RGB
import ru.hse.sd.rgb.utils.get
import kotlin.random.Random
import kotlin.random.nextInt

private object RandomRanges {
    val WIDTH_RANGE = 70..90
    val HEIGHT_RANGE = 25..40
    val MIN_SIZE_RANGE = 4..7
    val PASSAGE_RANGE = 3..5
    val COLOR_RANGE = 0..50
}

private const val MAX_HERO_CELL_RANDOM_ATTEMPTS = 1000
private const val DEFAULT_HERO_HP = 5
private val DEFAULT_HERO_COLOR = GameColor(RGB(40, 40, 40))
private const val DEFAULT_WALL_HP = 9999
private val DEFAULT_WALL_COLOR = GameColor(RGB(255, 255, 255))

fun generateLevel(): LevelDescription {
    val random = Random
    val maze = generateMaze(
        RandomRanges.WIDTH_RANGE,
        RandomRanges.HEIGHT_RANGE,
        RandomRanges.MIN_SIZE_RANGE,
        RandomRanges.PASSAGE_RANGE,
        random
    )
    val h = maze.size
    val w = maze[0].size

    val (r, g, b) = List(3) { random.nextInt(RandomRanges.COLOR_RANGE) }
    val bgColor = RGB(r, g, b)

    val entities = mutableSetOf<GameEntity>()
    for (x in 0 until w) for (y in 0 until h) if (maze[y][x]) entities.add(
        Wall(
            DEFAULT_WALL_COLOR,
            DEFAULT_WALL_HP,
            Cell(x, y)
        )
    )

    var heroCell: Cell? = null
    repeat(MAX_HERO_CELL_RANDOM_ATTEMPTS) {
        heroCell = Cell(random.nextInt(w), random.nextInt(h))
        if (!maze[heroCell!!])
            return@repeat
        heroCell = null
    }
    if (heroCell == null) throw IllegalStateException("too many attempts")
    val hero = Hero(setOf(ColorHpCell(DEFAULT_HERO_COLOR, DEFAULT_HERO_HP, heroCell!!)))
    entities.add(hero)

    return LevelDescription(w, h, entities, hero, bgColor)
}