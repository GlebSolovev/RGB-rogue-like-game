package ru.hse.sd.rgb.gameloaders

import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourEngine
import ru.hse.sd.rgb.gamelogic.engines.creation.CreationEngine
import ru.hse.sd.rgb.gamelogic.engines.fight.BaseColorStats
import ru.hse.sd.rgb.gamelogic.engines.fight.FightEngine
import ru.hse.sd.rgb.gamelogic.engines.items.ItemsEngine
import ru.hse.sd.rgb.gamelogic.engines.physics.PhysicsEngine
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Hero
import ru.hse.sd.rgb.utils.structures.Grid2D
import ru.hse.sd.rgb.utils.structures.RGB

data class GameWorldDescription(
    val gameGridW: Int,
    val gameGridH: Int,
    val allEntities: Set<GameEntity>, // include hero
    val gameBgColor: RGB, // can be ViewBackground
)

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
    val itemsEngine: ItemsEngine
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
    fun loadHero(): Hero
    fun loadLevelDescription(): LevelDescription
}

interface ColorLoader {
    fun loadColors(): BaseColorParams
}

class Loader(
    private val levelLoader: LevelLoader,
    private val colorLoader: ColorLoader
) {

    fun loadEngines(): Engines {
        val (w, h) = levelLoader.loadBasicParams()
        val (baseColorList, interactionMatrix) = colorLoader.loadColors()

        val physics = PhysicsEngine(w, h)
        val fighting = FightEngine(baseColorList, interactionMatrix)
        val creation = CreationEngine(physics, fighting)
        val behaviour = BehaviourEngine()
        val itemsEngine = ItemsEngine()

        return Engines(physics, fighting, creation, behaviour, itemsEngine)
    }

    fun loadHero(): Hero = levelLoader.loadHero()

    fun loadLevel(): LevelDescription = levelLoader.loadLevelDescription()
}
