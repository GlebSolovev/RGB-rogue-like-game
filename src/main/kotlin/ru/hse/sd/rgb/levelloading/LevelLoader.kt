package ru.hse.sd.rgb.levelloading

import ru.hse.sd.rgb.Cell
import ru.hse.sd.rgb.GameColor
import ru.hse.sd.rgb.WrongConfigError
import ru.hse.sd.rgb.entities.Hero
import ru.hse.sd.rgb.entities.Wall
import ru.hse.sd.rgb.entities.common.ColorCell
import ru.hse.sd.rgb.entities.common.GameEntity
import ru.hse.sd.rgb.levelloading.generators.generateLevel
import ru.hse.sd.rgb.views.RGB
import java.io.File
import java.util.*

data class LevelDescription(
    val w: Int,
    val h: Int,
    val entities: Set<GameEntity>,
    val bgColor: RGB, // can be ViewBackground
)

// loads random level if filename is null
fun loadLevel(filename: String?): LevelDescription {
    if (filename == null) return generateLevel()

    val file = File(filename)
    val scanner = Scanner(file)

    val w = scanner.nextInt()
    val h = scanner.nextInt()
    val entities = mutableSetOf<GameEntity>()

    val shortNameCount = scanner.nextInt()
    val shortNameBuilders = mutableMapOf<String, EntityBuilder>()
    repeat(shortNameCount) { readShortNameDescription(scanner, shortNameBuilders) }

    val colorCount = scanner.nextInt()
    val colorMap = mutableMapOf<String, GameColor>()
    repeat(colorCount) { readColorDescription(scanner, colorMap) }

    readEntitiesDescriptions(h, w, scanner, shortNameBuilders, colorMap, entities)

    for ((_, builder) in shortNameBuilders) {
        if (builder.isMulti) entities.add(builder.creator(builder.colorCells))
    }
    val bgColor = readRGB(scanner)
    return LevelDescription(w, h, entities, bgColor)
}

// ----------------- private -----------------

private const val HERO_NAME = "hero"
private const val WALL_NAME = "wall"

private data class EntityBuilder(val isMulti: Boolean, val creator: (Set<ColorCell>) -> GameEntity) {
    val colorCells = mutableSetOf<ColorCell>()
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
            EntityBuilder(isMulti) { s -> Hero(s) }
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
    h: Int,
    w: Int,
    scanner: Scanner,
    shortNameBuilders: MutableMap<String, EntityBuilder>,
    colorMap: MutableMap<String, GameColor>,
    entities: MutableSet<GameEntity>
) {
    for (y in 0 until h) {
        for (x in 0 until w) {
            val word = scanner.next()
            if (word == ".") continue
            if (word.length != 2) throw WrongConfigError("entity unit description length must be 2").also { println(word) }
            val entityShortName = word[0].toString()
            val colorName = word[1].toString()
            val entityBuilder = shortNameBuilders[entityShortName] ?: throw WrongConfigError("entity not found")
            val color = colorMap[colorName] ?: throw WrongConfigError("color not found")
            if (entityBuilder.isMulti) {
                entityBuilder.colorCells.add(ColorCell(Cell(x, y), color))
            } else {
                entities.add(entityBuilder.creator(setOf(ColorCell(Cell(x, y), color))))
            }
        }
    }
}
