package ru.hse.sd.rgb.gameloaders

import ru.hse.sd.rgb.gameloaders.factories.GenerationTable
import ru.hse.sd.rgb.gameloaders.factories.LevelContentFactory
import ru.hse.sd.rgb.gameloaders.generators.generateMaze
import ru.hse.sd.rgb.gamelogic.engines.items.scriptitems.ColorInverterEntity
import ru.hse.sd.rgb.gamelogic.engines.items.scriptitems.ColorModificationEntity
import ru.hse.sd.rgb.gamelogic.engines.items.scriptitems.InstantHealEntity
import ru.hse.sd.rgb.gamelogic.entities.ColorCellHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Hero
import ru.hse.sd.rgb.utils.nextChance
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
        maze = generateMaze(w, h, chamberMinSize, passageSize)

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

        for (x in 1 until w - 1) for (y in 1 until h - 1) if (maze[x, y]) entities.add(
            levelFactory.createWall(Cell(x, y))
        )
        // add outline walls
        for (x in 0 until w) {
            entities.add(levelFactory.createWall(Cell(x, 0)))
            entities.add(levelFactory.createWall(Cell(x, h - 1)))
        }
        for (y in 1 until h - 1) {
            entities.add(levelFactory.createWall(Cell(0, y)))
            entities.add(levelFactory.createWall(Cell(w - 1, y)))
        }

        repeat(levelFactory.sharpySpawnCount) {
            val emptyCells = getEmptyCells(w, h, entities)
            val sharpy = levelFactory.createSharpy(emptyCells.random())
            entities.add(sharpy)
        }

        maze.withCoords().forEach { (x, y, _) ->
            fun trySpawnRandomGeneratedEntity(chance: Double, createEntity: (Cell) -> GameEntity) {
                if (random.nextChance(chance)) {
                    val cell = Cell(x, y)
                    val currentEmptyCells = getEmptyCells(w, h, entities)
                    if (cell in currentEmptyCells) {
                        entities.add(createEntity(cell))
                    }
                }
            }
            trySpawnRandomGeneratedEntity(levelFactory.glitchSpawnRate) { cell ->
                levelFactory.createGlitch(cell)
            }
            trySpawnRandomGeneratedEntity(levelFactory.colorModificationSpawnRate) { cell ->
                val rgbDelta = levelFactory.colorModificationRGBDeltaGenerationTable.roll()
                ColorModificationEntity(cell, rgbDelta)
            }
            trySpawnRandomGeneratedEntity(levelFactory.instantHealSpawnRate) { cell ->
                InstantHealEntity(cell, levelFactory.instantHealGenerationTable.roll())
            }
        }

        repeat(levelFactory.colorInverterCountRate) {
            val emptyCells = getEmptyCells(w, h, entities)
            entities.add(ColorInverterEntity(emptyCells.random()))
        }

        return LevelDescription(
            GameWorldDescription(w, h, entities, levelFactory.bgColor),
            heroInventory
        )
    }

    private fun getEmptyCells(w: Int, h: Int, entities: Set<GameEntity>): Set<Cell> {
        val occupiedCells: Set<Cell> =
            entities.flatMap { entity -> entity.units.map<GameUnit, Cell> { it.cell } }.toSet()
        return Grid2D(w, h) { x, y -> Cell(x, y) }.toSet() subtract occupiedCells
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

                override val glitchSpawnRate = 0.0
                override val glitchHp = 1
                override val glitchClonePeriod = 5000L

                override val sharpySpawnCount: Int = 1
                override val sharpyColor: RGB = RGB(50, 50, 50)
                override val sharpyHp: Int = 5
                override val sharpyMovePeriodMillis: Long = 2000
                override val sharpySeeingDepth: Int = 5

                override val colorModificationSpawnRate = 1.0 / (30 * 30)
                override val colorModificationRGBDeltaGenerationTable = GenerationTable.builder<RGBDelta>()
                    .outcome(1) { RGBDelta(10, 0, 0) }
                    .outcome(1) { RGBDelta(0, 10, 0) }
                    .outcome(1) { RGBDelta(0, 0, 10) }
                    .outcome(1) { RGBDelta(-10, -10, -10) }
                    .build()

                override val instantHealSpawnRate = 1.0 / (40 * 40)
                override val instantHealGenerationTable = GenerationTable.builder<Int>()
                    .outcome(1) { 1 }
                    .build()

                override val colorInverterCountRate = 1
            }
        }
    }
}
