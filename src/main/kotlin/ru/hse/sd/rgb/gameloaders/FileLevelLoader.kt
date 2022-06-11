package ru.hse.sd.rgb.gameloaders

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gameloaders.factories.LevelContentFactory
import ru.hse.sd.rgb.gameloaders.factories.OverloadableFactory
import ru.hse.sd.rgb.gamelogic.entities.ColorCellHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.*
import ru.hse.sd.rgb.utils.WrongConfigError
import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.utils.structures.Grid2D
import ru.hse.sd.rgb.utils.structures.RGB
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
abstract class GameEntityDescription {
    abstract fun createEntity(): GameEntity
}

// TODO: add default entities descriptions (best if with access to factory)

@Serializable
@SerialName("sharpy")
class SharpyDescription(
    private val cell: Cell,
    private val color: RGB,
    private val hp: Int,
    private val movePeriodMillis: Long,
    private val seeingDepth: Int,
) : GameEntityDescription() {
    override fun createEntity() = Sharpy(
        ColorCellHp(color, cell, hp), movePeriodMillis, seeingDepth, controller.fighting.newTeamId()
    )
}

@Serializable
@SerialName("glitch")
class GlitchDescription(
    private val cell: Cell,
    private val hp: Int,
    private val clonePeriodMillis: Long,
) : GameEntityDescription() {
    override fun createEntity() = Glitch(
        cell, hp, clonePeriodMillis, controller.fighting.newTeamId()
    )
}

@Serializable
data class LevelPortalDescription(
    val cell: Cell,
    val nextLevelDescriptionFilename: String,
    val heroExperienceLevelToEnableOn: Int
)

@Serializable
@SerialName("custom")
data class FileCustomLevelDescription(
    val levelFactory: OverloadableFactory,
    val mazeRepresentation: List<String>,
    val heroSpawnCell: Cell,
    val customEntities: List<GameEntityDescription>,
    val levelPortalDescription: LevelPortalDescription? = null,
) : FileLevelDescription()

class FileLevelLoader(desc: FileCustomLevelDescription, private val random: Random = Random) : LevelLoader {

    private val levelFactory: LevelContentFactory
    private val maze: Grid2D<Boolean>
    private val heroSpawnCell: Cell
    private val customEntitiesDescriptions: List<GameEntityDescription>
    private val levelPortalDescription: LevelPortalDescription?

    private var hero: Hero? = null
    private lateinit var invDesc: InventoryDescription

    private val allEntities = mutableSetOf<GameEntity>()

    init {
        levelFactory = desc.levelFactory
        maze = parseMaze(desc.mazeRepresentation)
        heroSpawnCell = desc.heroSpawnCell
        customEntitiesDescriptions = desc.customEntities
        levelPortalDescription = desc.levelPortalDescription
    }

    override fun loadBasicParams() = LevelBasicParams(maze.w, maze.h)

    override fun populateHero(heroPersistence: HeroPersistence): Hero {
        hero = Hero(heroSpawnCell, heroPersistence)
        invDesc = heroPersistence.inventoryPersistence.description
        allEntities.add(hero!!)
        return hero!!
    }

    override fun loadLevelDescription(): LevelDescription {
        hero ?: error("loadHero() has not been called yet")
        allEntities.addAll(createLevelEntities(maze.w, maze.h, maze, levelFactory, null, random))
        allEntities.addAll(
            customEntitiesDescriptions.map { it.createEntity() }
        )
        if (levelPortalDescription != null) {
            allEntities.add(
                LevelPortal(
                    levelPortalDescription.cell,
                    levelPortalDescription.nextLevelDescriptionFilename,
                    levelPortalDescription.heroExperienceLevelToEnableOn
                )
            )
        }
        return LevelDescription(
            GameWorldDescription(maze.w, maze.h, allEntities, levelFactory.bgColor),
            invDesc
        )
    }
}

private fun parseMaze(mazeRepresentation: List<String>): Grid2D<Boolean> = Grid2D(
    mazeRepresentation.map { line ->
        line.map { c ->
            when (c) {
                '.' -> false
                '#' -> true
                else -> throw WrongConfigError("symbol $c means nether empty space nor wall")
            }
        }
    }
)
