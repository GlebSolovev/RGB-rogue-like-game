package ru.hse.sd.rgb.gamelogic

import ru.hse.sd.rgb.MockView
import ru.hse.sd.rgb.awaitLoadedLevel
import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gameloaders.*
import ru.hse.sd.rgb.gameloaders.factories.OverloadableFactory
import ru.hse.sd.rgb.gameloaders.factories.OverloadableFactoryType
import ru.hse.sd.rgb.gameloaders.generators.generateMaze
import ru.hse.sd.rgb.gamelogic.engines.experience.Experience
import ru.hse.sd.rgb.gamelogic.engines.items.InventoryPersistence
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.HeroPersistence
import ru.hse.sd.rgb.utils.messaging.messages.StartControllerMessage
import ru.hse.sd.rgb.utils.messaging.messages.UserQuit
import ru.hse.sd.rgb.utils.structures.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LevelGeneratorTest {

    private val random = Random(5)

    @Test
    fun testStressGeneration() {
        repeat(1000) { // RepeatedTest doesn't have a fail-fast option
            val w = random.nextInt(30, 60)
            val h = random.nextInt(30, 60)
            val minPathSize = random.nextInt(3, 10)
            val passageSize = random.nextInt(2, 4)

            val maze = generateMaze(w, h, minPathSize, passageSize, random)

            val emptyCells = maze
                .withCoords()
                .asSequence()
                .filterNot { (_, _, v) -> v }
                .map { (x, y, _) -> Cell(x, y) }
                .toSet()
            val beenCells = mutableSetOf<Cell>()

            fun dfs(cell: Cell) {
                if (maze[cell]) return
                if (cell in beenCells) return
                beenCells.add(cell)
                for (dir in Direction.values()) {
                    val next = cell + dir.toShift()
                    if (next.x in 0 until w && next.y in 0 until h) {
                        dfs(next)
                    }
                }
            }

            val start = emptyCells.random(random)
            dfs(start)

            assertEquals(emptyCells, beenCells)
        }
    }

    private val folder = "src/test/resources"

    @Test
    fun testStressEntities() = repeat(100) {
        runBlocking {
            controller = Controller(
                FileLevelLoader("$folder/emptyLevel.yaml"),
                FileColorLoader("$folder/integration/colorsBasic.yaml"),
                FileExperienceLevelsLoader("$folder/integration/experience.yaml"),
                FileHeroLoader("$folder/integration/hero.yaml"),
                MockView()
            )

            controller.receive(StartControllerMessage())
            launch { controller.messagingRoutine() }
            controller.awaitLoadedLevel("$folder/emptyLevel.yaml")

            val fakeHeroPersistence = HeroPersistence(
                listOf(HeroPersistence.HpUnitPersistence(GridShift(0, 0), RGB(0, 0, 0), 1, 1)),
                InventoryPersistence(1, 1),
                0,
                Experience(0, 0)
            )
            val w = random.nextInt(30, 60)
            val h = random.nextInt(30, 60)
            val minPathSize = random.nextInt(3, 10)
            val passageSize = random.nextInt(2, 4)

            val levelLoader = RandomLevelLoader.builder(random) {
                width = w
                height = h
                chamberMinSize = minPathSize
                this.passageSize = passageSize
                factory = OverloadableFactory(
                    OverloadableFactoryType.FIERY,
                    customGlitchSpawnCount = 10,
                    customSharpySpawnCount = 10,
                    customColorInverterSpawnCount = 10,
                    customColorModificationSpawnCount = 10,
                    customInstantHealSpawnCount = 10
                )
            }
            levelLoader.loadBasicParams()
            levelLoader.populateHero(fakeHeroPersistence)
            val levelDesc = levelLoader.loadLevelDescription()
            val gameDesc = levelDesc.gameDesc

            val entitiesGrid = Grid2D(w, h) { _, _ -> mutableSetOf<GameEntity>() }
            for (e in gameDesc.allEntities) {
                for (unit in e.units) entitiesGrid[unit.cell].add(e)
            }
            for (set in entitiesGrid) {
                assertTrue(set.toString()) { set.size <= 1 }
            }

            controller.receive(UserQuit())
        }
    }

}
