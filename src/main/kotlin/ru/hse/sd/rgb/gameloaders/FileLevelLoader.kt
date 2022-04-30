import ru.hse.sd.rgb.gameloaders.*
import ru.hse.sd.rgb.gamelogic.engines.fight.GameColor
import ru.hse.sd.rgb.gamelogic.entities.ColorHpCell
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Hero
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Wall
import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.utils.RGB
import ru.hse.sd.rgb.utils.WrongConfigError
import java.io.File
import java.util.*

class FileLevelLoader(levelFilename: String) : LevelLoader {

    private val scanner = Scanner(File(levelFilename))
    private var basicParams: LevelBasicParams? = null

    override fun loadBasicParams(): LevelBasicParams {
        val w = scanner.nextInt()
        val h = scanner.nextInt()
        basicParams = LevelBasicParams(w, h)
        return basicParams!!
    }

    override fun loadLevelDescription(): LevelDescription {
        val (w, h) = basicParams ?: throw IllegalStateException("loadBasicParams() has not been called yet")
        val entities = mutableSetOf<GameEntity>()

        val shortNameCount = scanner.nextInt()
        val shortNameBuilders = mutableMapOf<String, EntityBuilder>()
        repeat(shortNameCount) { readShortNameDescription(scanner, shortNameBuilders) }

        val colorCount = scanner.nextInt()
        val colorMap = mutableMapOf<String, GameColor>()
        repeat(colorCount) { readColorDescription(scanner, colorMap) }

        readEntitiesDescriptions(w, h, scanner, shortNameBuilders, colorMap, entities)

        for ((_, builder) in shortNameBuilders) {
            if (builder.isMulti) entities.add(builder.creator(builder.colorCells))
        }
        val bgColor = readRGB(scanner)
        val hero = entities.find { it is Hero }!! as Hero
        return LevelDescription(
            GameWorldDescription(w, h, entities, hero, bgColor),
            INV_DESC,
        )
    }

    // ----------------- private -----------------

    private val HERO_NAME = "hero"
    private val WALL_NAME = "wall"

    private val DEFAULT_HP = 10 // TODO: read from config
    private val INV_DESC = InventoryDescription(5, 5) // TODO: read from config

    private data class EntityBuilder(val isMulti: Boolean, val creator: (Set<ColorHpCell>) -> GameEntity) {
        val colorCells = mutableSetOf<ColorHpCell>()
    }

    private fun readRGB(scanner: Scanner) = RGB(scanner.nextInt(), scanner.nextInt(), scanner.nextInt())

    private fun readShortNameDescription(scanner: Scanner, shortNameBuilders: MutableMap<String, EntityBuilder>) {
        val shortName = scanner.next()
        if (shortName.length != 1) throw WrongConfigError("short name length must be 1")
        val entityName = scanner.next()
        val isMulti = when (scanner.next()) {
            "single" -> false
            "multi" -> true
            else -> throw WrongConfigError("unknown tag")
        }
        shortNameBuilders[shortName] = when (entityName) {
            HERO_NAME -> {
                EntityBuilder(isMulti) { s -> Hero(s, INV_DESC) }
            }
            WALL_NAME -> {
                EntityBuilder(isMulti) { s -> Wall(s) }
            }
            else -> throw WrongConfigError("unknown entity")
        }
    }

    private fun readColorDescription(scanner: Scanner, colorMap: MutableMap<String, GameColor>) {
        val colorName = scanner.next()
        if (colorName.length != 1) throw WrongConfigError("color name length must be 1")
        val rgb = readRGB(scanner)
        colorMap[colorName] = GameColor(rgb)
    }

    private fun readEntitiesDescriptions(
        w: Int,
        h: Int,
        scanner: Scanner,
        shortNameBuilders: MutableMap<String, EntityBuilder>,
        colorMap: MutableMap<String, GameColor>,
        entities: MutableSet<GameEntity>
    ) {
        for (y in 0 until h) {
            for (x in 0 until w) {
                val word = scanner.next()
                if (word == ".") continue
                if (word.length != 2) throw WrongConfigError("entity unit description length must be 2").also {
                    println(
                        word
                    )
                }
                val entityShortName = word[0].toString()
                val colorName = word[1].toString()
                val entityBuilder = shortNameBuilders[entityShortName] ?: throw WrongConfigError("entity not found")
                val color = colorMap[colorName] ?: throw WrongConfigError("color not found")
                if (entityBuilder.isMulti) {
                    entityBuilder.colorCells.add(ColorHpCell(color, DEFAULT_HP, Cell(x, y)))
                } else {
                    entities.add(entityBuilder.creator(setOf(ColorHpCell(color, DEFAULT_HP, Cell(x, y)))))
                }
            }
        }
    }

}


