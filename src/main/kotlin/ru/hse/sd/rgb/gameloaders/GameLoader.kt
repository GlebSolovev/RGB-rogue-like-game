package ru.hse.sd.rgb.gameloaders

import ru.hse.sd.rgb.gameloaders.factories.LevelContentFactory
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourEngine
import ru.hse.sd.rgb.gamelogic.engines.creation.CreationEngine
import ru.hse.sd.rgb.gamelogic.engines.experience.ExperienceEngine
import ru.hse.sd.rgb.gamelogic.engines.experience.ExperienceLevelDescription
import ru.hse.sd.rgb.gamelogic.engines.fight.BaseColorStats
import ru.hse.sd.rgb.gamelogic.engines.fight.FightEngine
import ru.hse.sd.rgb.gamelogic.engines.items.ItemsEngine
import ru.hse.sd.rgb.gamelogic.engines.physics.PhysicsEngine
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Hero
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.HeroPersistence
import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.utils.structures.Grid2D
import ru.hse.sd.rgb.utils.structures.RGB
import kotlinx.serialization.Serializable
import kotlin.random.Random

data class GameWorldDescription(
    val gameGridW: Int,
    val gameGridH: Int,
    val allEntities: Set<GameEntity>, // include hero
    val gameBgColor: RGB, // can be ViewBackground
)

@Serializable
data class InventoryDescription(
    val invGridW: Int,
    val invGridH: Int,
)

data class LevelDescription(
    val gameDesc: GameWorldDescription,
    val invDesc: InventoryDescription,
)

data class Engines(
    val physics: PhysicsEngine,
    val fighting: FightEngine,
    val creation: CreationEngine,
    val behaviourEngine: BehaviourEngine,
    val itemsEngine: ItemsEngine,
    val experience: ExperienceEngine
)

data class LevelBasicParams(
    val width: Int,
    val height: Int,
)

data class BaseColorParams(
    val baseColorList: List<BaseColorStats>,
    val interactionMatrix: Grid2D<Int>
)

interface LevelLoader {
    fun loadBasicParams(): LevelBasicParams
    fun populateHero(heroPersistence: HeroPersistence): Hero
    fun loadLevelDescription(): LevelDescription
}

interface ColorLoader {
    fun loadColors(): BaseColorParams
}

interface ExperienceLevelsLoader {
    fun loadHeroExperienceLevels(): List<ExperienceLevelDescription>
}

class GameLoader(
    private val levelLoader: LevelLoader,
    private val colorLoader: ColorLoader,
    private val experienceLevelsLoader: ExperienceLevelsLoader
) {

    fun loadEngines(): Engines {
        val (w, h) = levelLoader.loadBasicParams()
        val (baseColorList, interactionMatrix) = colorLoader.loadColors()
        val heroExperienceLevels = experienceLevelsLoader.loadHeroExperienceLevels()

        val physics = PhysicsEngine(w, h)
        val fighting = FightEngine(baseColorList, interactionMatrix)
        val creation = CreationEngine(physics, fighting)
        val behaviour = BehaviourEngine()
        val itemsEngine = ItemsEngine()
        val experience = ExperienceEngine(heroExperienceLevels)

        return Engines(physics, fighting, creation, behaviour, itemsEngine, experience)
    }

    fun populateHero(heroPersistence: HeroPersistence): Hero = levelLoader.populateHero(heroPersistence)

    fun loadLevel(): LevelDescription = levelLoader.loadLevelDescription()
}

@Suppress("unused") // instead of visibility modifier
fun LevelLoader.createLevelEntities(
    w: Int,
    h: Int,
    maze: Grid2D<Boolean>,
    levelFactory: LevelContentFactory,
    random: Random = Random
): Set<GameEntity> {
    val entities = mutableSetOf<GameEntity>()

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

    fun Sequence<Cell>.spawnWrapper(count: Int, action: (Cell) -> GameEntity): Sequence<Cell> {
        take(count).forEach { entities.add(action(it)) }
        return drop(count)
    }

    val occupiedCells = entities.flatMap { entity -> entity.units.map { it.cell } }.toSet()
    val emptyCells = Grid2D(w, h) { x, y -> Cell(x, y) }.toSet() subtract occupiedCells

    emptyCells
        .asSequence()
        .shuffled(random)
        .spawnWrapper(levelFactory.glitchSpawnCount) { levelFactory.createGlitch(it) }
        .spawnWrapper(levelFactory.sharpySpawnCount) { levelFactory.createSharpy(it) }
        .spawnWrapper(levelFactory.colorModificationSpawnCount) { levelFactory.createColorModification(it) }
        .spawnWrapper(levelFactory.instantHealSpawnCount) { levelFactory.createInstantHeal(it) }
        .spawnWrapper(levelFactory.colorInverterSpawnCount) { levelFactory.createColorInverter(it) }
        .firstOrNull() ?: error("not enough empty cells to spawn all entities") // force lazy sequence operations

    return entities
}
