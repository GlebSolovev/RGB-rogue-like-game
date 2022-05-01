package ru.hse.sd.rgb.gameloaders

import ru.hse.sd.rgb.gameloaders.factories.LevelContentFactory
import ru.hse.sd.rgb.gameloaders.generators.generateMaze
import ru.hse.sd.rgb.gamelogic.engines.fight.GameColor
import ru.hse.sd.rgb.gamelogic.entities.ColorHpCell
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Hero
import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.utils.RGB
import kotlin.random.Random

class RandomLevelLoader private constructor(
    private val width: Int,
    private val height: Int,
    private val chamberMinSize: Int,
    private val passageSize: Int,
    private val levelFactory: LevelContentFactory,
    private val heroHp: Int,
    private val heroColor: RGB,
    private val heroInventory: InventoryDescription,
    private val generatorRandom: Random
) : LevelLoader {

    private val MAX_HERO_CELL_RANDOM_ATTEMPTS = 1000000

    private var basicParams: LevelBasicParams? = null

    override fun loadBasicParams(): LevelBasicParams {
        basicParams = LevelBasicParams(width, height)
        return basicParams!!
    }

    override fun loadLevelDescription(): LevelDescription {
        val (w, h) = basicParams ?: throw IllegalStateException("loadBasicParams() has not been called yet")
        val maze = generateMaze(w, h, chamberMinSize, passageSize)

        val entities = mutableSetOf<GameEntity>()
        for (x in 0 until w) for (y in 0 until h) if (maze[x, y]) entities.add(
            levelFactory.createWall(Cell(x, y))
        )

        var heroCell: Cell? = null
        repeat(MAX_HERO_CELL_RANDOM_ATTEMPTS) {
            heroCell = Cell(generatorRandom.nextInt(w), generatorRandom.nextInt(h))
            if (!maze[heroCell!!])
                return@repeat
            heroCell = null
        }
        if (heroCell == null) throw IllegalStateException("too many attempts") // TODO: happens a lot
        val hero = Hero(
            setOf(ColorHpCell(GameColor(heroColor), heroHp, heroCell!!)),
            heroInventory
        )
        entities.add(hero)

        return LevelDescription(
            GameWorldDescription(w, h, entities, hero, levelFactory.bgColor),
            heroInventory
        )
    }

    // ------------ builder ------------

    companion object {
        fun builder(block: Builder.() -> Unit) = builder(Random, block)
        fun builder(random: Random, block: Builder.() -> Unit) = Builder(random, block).build()
    }

    class Builder private constructor(private val random: Random) {

        constructor(random: Random, init: Builder.() -> Unit) : this(random) {
            init()
        }

        fun random(range: IntRange) = random.nextInt(range.first, range.last)

        var width: Int = random(DefaultParams.WIDTH_RANGE)
        var height: Int = random(DefaultParams.HEIGHT_RANGE)
        var chamberMinSize: Int = random(DefaultParams.MIN_SIZE_RANGE)
        var passageSize: Int = random(DefaultParams.PASSAGE_RANGE)

        var factory: LevelContentFactory = DefaultParams.LEVEL_FACTORY
        var heroHp: Int = DefaultParams.HERO_HP
        var heroColor: RGB = DefaultParams.HERO_COLOR
        var heroInventory: InventoryDescription = DefaultParams.INV_DESC

        fun build(): LevelLoader = RandomLevelLoader(
            width = width,
            height = height,
            chamberMinSize = chamberMinSize,
            passageSize = passageSize,
            levelFactory = factory,
            heroHp = heroHp,
            heroColor = heroColor,
            heroInventory = heroInventory,
            generatorRandom = random
        )

        private object DefaultParams {
            val WIDTH_RANGE = 70..90
            val HEIGHT_RANGE = 25..40
            val MIN_SIZE_RANGE = 4..7
            val PASSAGE_RANGE = 3..5

            const val HERO_HP = 5
            val HERO_COLOR = RGB(250, 0, 0)
            val INV_DESC = InventoryDescription(5, 5)

            val LEVEL_FACTORY = object : LevelContentFactory() {
                override val bgColor: RGB = RGB(0, 0, 0)
                override val wallColor: RGB = RGB(100, 100, 100)
                override val wallHp: Int = 999
            }
        }
    }

}