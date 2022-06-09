package ru.hse.sd.rgb.gameloaders

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gameloaders.factories.LevelContentFactory
import ru.hse.sd.rgb.gameloaders.factories.OverloadableFactory
import ru.hse.sd.rgb.gamelogic.entities.ColorCellHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Hero
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.HeroPersistence
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.LevelPortal
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Sharpy
import ru.hse.sd.rgb.utils.WrongConfigError
import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.utils.structures.Grid2D
import ru.hse.sd.rgb.utils.structures.RGB
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.io.File
import kotlin.random.Random

@Serializable
abstract class GameEntityDescription {
    abstract fun createEntity(): GameEntity
}

private val entitiesSerializersModule = SerializersModule {
    polymorphic(GameEntityDescription::class) {
        subclass(SharpyDescription::class)
    }
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
data class LevelPortalDescription(
    val cell: Cell,
    val nextLevelDescriptionFilename: String,
    val heroExperienceLevelToEnableOn: Int
)

@Serializable
data class FileLevelDescription(
    val levelFactory: OverloadableFactory,
    val mazeRepresentation: List<String>,
    val heroSpawnCell: Cell,
    val customEntities: List<GameEntityDescription>,
    val levelPortalDescription: LevelPortalDescription? = null,
)

class FileLevelLoader(val filename: String, private val random: Random = Random) : LevelLoader {

    private val levelFactory: LevelContentFactory
    private val maze: Grid2D<Boolean>
    private val heroSpawnCell: Cell
    private val customEntitiesDescriptions: List<GameEntityDescription>
    private val levelPortalDescription: LevelPortalDescription?

    private var hero: Hero? = null
    private lateinit var invDesc: InventoryDescription

    private val allEntities = mutableSetOf<GameEntity>()

    init {
        val format = Yaml(serializersModule = entitiesSerializersModule)
        val desc = format.decodeFromStream<FileLevelDescription>(File(filename).inputStream())
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
        allEntities.addAll(createLevelEntities(maze.w, maze.h, maze, levelFactory, random))
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
