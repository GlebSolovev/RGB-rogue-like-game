package ru.hse.sd.rgb.gameloaders

import ru.hse.sd.rgb.gameloaders.factories.GenerationTable
import ru.hse.sd.rgb.gameloaders.factories.LevelContentFactory
import ru.hse.sd.rgb.gameloaders.generators.generateMaze
import ru.hse.sd.rgb.gamelogic.entities.ColorCellHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Hero
import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.utils.structures.Grid2D
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.utils.structures.RGBDelta
import kotlin.random.Random

@Suppress("LongParameterList")
class RandomLevelLoader private constructor(
    private val width: Int,
    private val height: Int,
    private val chamberMinSize: Int,
    private val passageSize: Int,
    private val levelFactory: LevelContentFactory,
    private val heroHp: Int,
    private val heroColor: RGB,
    private val heroInventory: InventoryDescription,
    private val heroMovePeriodLimit: Long,
    private val random: Random
) : LevelLoader {

    private var basicParams: LevelBasicParams? = null
    private var maze: Grid2D<Boolean>? = null
    private val entities = mutableSetOf<GameEntity>()

    override fun loadBasicParams(): LevelBasicParams {
        basicParams = LevelBasicParams(width, height)
        return basicParams!!
    }

    override fun loadHero(): Hero {
        val (w, h) = basicParams ?: throw IllegalStateException("loadBasicParams() has not been called yet")
        maze = generateMaze(w, h, chamberMinSize, passageSize, random)

        val heroCell = maze!!.withCoords().asSequence()
            .filterNot { it.value }
            .filterNot { it.x == 0 || it.x == w - 1 || it.y == 0 || it.y == h - 1 }
            .map { (x, y, _) -> Cell(x, y) }
            .shuffled()
            .first()
        val hero = Hero(
            setOf(ColorCellHp(heroColor, heroCell, heroHp)),
            heroInventory,
            heroMovePeriodLimit
        )
        entities.add(hero)
        return hero
    }

    override fun loadLevelDescription(): LevelDescription {
        val (w, h) = basicParams ?: throw IllegalStateException("loadBasicParams() has not been called yet")
        val maze = maze ?: throw IllegalStateException("loadHero() has not been called yet")

        val entities = createLevelEntities(w, h, maze, levelFactory, random)

        return LevelDescription(
            GameWorldDescription(w, h, entities, levelFactory.bgColor),
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
        var heroMovePeriodLimit: Long = DefaultParams.HERO_MOVE_PERIOD_LIMIT

        fun build(): LevelLoader = RandomLevelLoader(
            width = width,
            height = height,
            chamberMinSize = chamberMinSize,
            passageSize = passageSize,
            levelFactory = factory,
            heroHp = heroHp,
            heroColor = heroColor,
            heroInventory = heroInventory,
            heroMovePeriodLimit = heroMovePeriodLimit,
            random = random
        )

        @Suppress("MagicNumber")
        private object DefaultParams {
            val WIDTH_RANGE = 70..90
            val HEIGHT_RANGE = 25..40
            val MIN_SIZE_RANGE = 5..10
            val PASSAGE_RANGE = 3..6

            const val HERO_HP = 10
            val HERO_COLOR = RGB(220, 0, 0)
            val INV_DESC = InventoryDescription(5, 5)
            const val HERO_MOVE_PERIOD_LIMIT = 50L

            val LEVEL_FACTORY = object : LevelContentFactory() {
                override val bgColor: RGB = RGB(0, 0, 0)
                override val wallColor: RGB = RGB(100, 100, 100)

                override val glitchSpawnCount = 0
                override val glitchHp = 1
                override val glitchClonePeriod = 5000L

                override val sharpySpawnCount: Int = 1
                override val sharpyColor: RGB = RGB(50, 50, 50)
                override val sharpyHp: Int = 5
                override val sharpyMovePeriodMillis: Long = 2000
                override val sharpySeeingDepth: Int = 5

                override val colorModificationSpawnCount = 5
                override val colorModificationRGBDeltaGenerationTable = GenerationTable.builder<RGBDelta>()
                    .outcome(1) { RGBDelta(10, 0, 0) }
                    .outcome(1) { RGBDelta(0, 10, 0) }
                    .outcome(1) { RGBDelta(0, 0, 10) }
                    .outcome(1) { RGBDelta(-10, -10, -10) }
                    .build()

                override val instantHealSpawnCount = 5
                override val instantHealGenerationTable = GenerationTable.builder<Int>()
                    .outcome(1) { 1 }
                    .build()

                override val colorInverterSpawnCount = 1
            }
        }
    }
}
