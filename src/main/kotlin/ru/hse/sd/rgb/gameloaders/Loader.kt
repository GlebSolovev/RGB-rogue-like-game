package ru.hse.sd.rgb.gameloaders

import FileLevelLoader
import ru.hse.sd.rgb.gamelogic.engines.creation.CreationEngine
import ru.hse.sd.rgb.gamelogic.engines.fight.FightEngine
import ru.hse.sd.rgb.gamelogic.engines.physics.PhysicsEngine
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Hero
import ru.hse.sd.rgb.utils.RGB
import java.util.concurrent.ConcurrentHashMap

data class GameWorldDescription(
    val gameGridW: Int,
    val gameGridH: Int,
    val allEntities: Set<GameEntity>, // include hero
    val hero: Hero,
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
)

data class LevelBasicParams(
    val width: Int,
    val height: Int,
)

interface LevelLoader {
    fun loadBasicParams(): LevelBasicParams
    fun loadLevelDescription(): LevelDescription
}

class Loader(levelFilename: String?, colorsFilename: String?) {

    private val levelLoader = if (levelFilename != null) FileLevelLoader(levelFilename) else RandomLevelLoader()
    private val colorLoader = ColorLoader(colorsFilename) // TODO

    fun loadEngines(): Engines {
        val (w, h) = levelLoader.loadBasicParams()

        val physics = PhysicsEngine(w, h)
        val fighting = FightEngine(ConcurrentHashMap(), ConcurrentHashMap()) // TODO: pass loaded maps
        val creation = CreationEngine(physics, fighting)

        return Engines(physics, fighting, creation)
    }

    fun loadLevel(): LevelDescription = levelLoader.loadLevelDescription()

}
