package ru.hse.sd.rgb.gameloaders

import ru.hse.sd.rgb.gameloaders.factories.LevelContentFactory
import ru.hse.sd.rgb.gameloaders.generators.generateMaze
import ru.hse.sd.rgb.gamelogic.entities.ColorCellHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Hero
import ru.hse.sd.rgb.utils.*
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
    private val heroMovePeriodLimit: Long,
    private val random: Random
) : LevelLoader {

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

        val heroCell: Cell = getEmptyCells(w, h, entities).randomElement(random)
            ?: throw IllegalStateException("no empty cells to spawn hero")
        val hero = Hero(
            setOf(ColorCellHp(heroColor, heroCell, heroHp)),
            heroInventory,
            heroMovePeriodLimit
        )
        entities.add(hero)

        val currentEmptyCells = getEmptyCells(w, h, entities)
        maze.withCoords().forEach { (x, y, _) ->
            if (random.nextChance(levelFactory.glitchSpawnRate)) {
                val cell = Cell(x, y)
                if (cell in currentEmptyCells) {
                    val glitch = levelFactory.createGlitch(cell)
                    entities.add(glitch)
                }
            }
        }

        return LevelDescription(
            GameWorldDescription(w, h, entities, hero, levelFactory.bgColor),
            heroInventory
        )
    }

    private fun getEmptyCells(w: Int, h: Int, entities: Set<GameEntity>): Set<Cell> {
        val occupiedCells: Set<Cell> = entities.flatMap { it.units.map { it.cell } }.toSet()
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

        private object DefaultParams {
            val WIDTH_RANGE = 70..90
            val HEIGHT_RANGE = 25..40
            val MIN_SIZE_RANGE = 4..7
            val PASSAGE_RANGE = 3..5

            const val HERO_HP = 10
            val HERO_COLOR = RGB(220, 0, 0)
            val INV_DESC = InventoryDescription(5, 5)
            const val HERO_MOVE_PERIOD_LIMIT = 50L

            val LEVEL_FACTORY = object : LevelContentFactory() {
                override val bgColor: RGB = RGB(0, 0, 0)
                override val wallColor: RGB = RGB(100, 100, 100)
                override val glitchHp = 1
                override val glitchSpawnRate = 0.0
            }
        }
    }

}