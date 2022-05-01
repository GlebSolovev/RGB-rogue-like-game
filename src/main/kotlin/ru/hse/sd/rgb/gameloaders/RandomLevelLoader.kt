package ru.hse.sd.rgb.gameloaders

import ru.hse.sd.rgb.gameloaders.generators.generateMaze
import ru.hse.sd.rgb.gamelogic.engines.fight.GameColor
import ru.hse.sd.rgb.gamelogic.entities.ColorHpCell
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Hero
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Wall
import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.utils.RGB
import kotlin.random.Random
import kotlin.random.nextInt

class RandomLevelLoader(private val random: Random = Random) : LevelLoader {

    private object RandomParams {
        val WIDTH_RANGE = 70..90
        val HEIGHT_RANGE = 25..40
        val MIN_SIZE_RANGE = 4..7
        val PASSAGE_RANGE = 3..5
        val COLOR_RANGE = 0..50

        const val MAX_HERO_CELL_RANDOM_ATTEMPTS = 1000000
        const val DEFAULT_HERO_HP = 5
        val DEFAULT_HERO_COLOR = RGB(250, 0, 0)
        const val DEFAULT_WALL_HP = 9999
        val DEFAULT_WALL_COLOR = RGB(255, 255, 255)
        val DEFAULT_INV_DESC = InventoryDescription(5, 5)
    }

    private var basicParams: LevelBasicParams? = null

    override fun loadBasicParams(): LevelBasicParams {
        basicParams = LevelBasicParams(
            random.nextInt(RandomParams.WIDTH_RANGE),
            random.nextInt(RandomParams.HEIGHT_RANGE),
        )
        return basicParams!!
    }

    override fun loadLevelDescription(): LevelDescription {
        val (w, h) = basicParams ?: throw IllegalStateException("loadBasicParams() has not been called yet")
        val maze = generateMaze(
            w, h,
            RandomParams.MIN_SIZE_RANGE,
            RandomParams.PASSAGE_RANGE,
            random
        )

        val (r, g, b) = List(3) { random.nextInt(RandomParams.COLOR_RANGE) }
        val bgColor = RGB(r, g, b)

        val entities = mutableSetOf<GameEntity>()
        for (x in 0 until w) for (y in 0 until h) if (maze[x, y]) entities.add(
            Wall(
                GameColor(RandomParams.DEFAULT_WALL_COLOR),
                RandomParams.DEFAULT_WALL_HP,
                Cell(x, y)
            )
        )

        var heroCell: Cell? = null
        repeat(RandomParams.MAX_HERO_CELL_RANDOM_ATTEMPTS) {
            heroCell = Cell(random.nextInt(w), random.nextInt(h))
            if (!maze[heroCell!!])
                return@repeat
            heroCell = null
        }
        if (heroCell == null) throw IllegalStateException("too many attempts") // TODO: happens a lot
        val hero = Hero(
            setOf(ColorHpCell(GameColor(RandomParams.DEFAULT_HERO_COLOR), RandomParams.DEFAULT_HERO_HP, heroCell!!)),
            RandomParams.DEFAULT_INV_DESC
        )
        entities.add(hero)

        return LevelDescription(
            GameWorldDescription(w, h, entities, hero, bgColor),
            RandomParams.DEFAULT_INV_DESC
        )
    }

}