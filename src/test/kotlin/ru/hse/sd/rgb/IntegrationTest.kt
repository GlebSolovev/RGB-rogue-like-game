package ru.hse.sd.rgb

import ru.hse.sd.rgb.gameloaders.*
import ru.hse.sd.rgb.gamelogic.Controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuilder
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.EnableColorUpdate
import ru.hse.sd.rgb.gamelogic.engines.experience.Experience
import ru.hse.sd.rgb.gamelogic.engines.fight.AttackType
import ru.hse.sd.rgb.gamelogic.engines.fight.ControlParams
import ru.hse.sd.rgb.gamelogic.engines.fight.HealType
import ru.hse.sd.rgb.gamelogic.engines.items.InventoryViewSnapshot
import ru.hse.sd.rgb.gamelogic.entities.ColorCellHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.gamelogic.entities.HpGameUnit
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.*
import ru.hse.sd.rgb.gamelogic.exceptionStackTrace
import ru.hse.sd.rgb.utils.ignore
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.messages.*
import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.utils.structures.Direction
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.utils.unreachable
import ru.hse.sd.rgb.views.DrawablesMap
import ru.hse.sd.rgb.views.View
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape
import kotlinx.coroutines.*
import org.junit.jupiter.api.RepeatedTest
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntegrationTest {

    private val filesFolder = "src/test/resources/integration"

    @RepeatedTest(5)
    fun testBasic(): Unit = runBlocking {
        val mockView = MockView()
        controller = Controller(
            loadLevelLoader("$filesFolder/level1.yaml"),
            FileColorLoader("$filesFolder/colorsBasic.yaml"),
            FileExperienceLevelsLoader("$filesFolder/experience.yaml"),
            FileHeroLoader("$filesFolder/hero.yaml"),
            mockView
        )
        controller.receive(StartControllerMessage())
        val controllerJob = launch { controller.messagingRoutine() }

        withTimeout(2000) {
            controller.awaitLoadedLevel("$filesFolder/level1.yaml")
        }

        suspend fun almostCycleThroughLevel1() {
            repeat(10) { mockView.simulateUserMove(Direction.RIGHT) }
            mockView.simulateUserMove(Direction.DOWN)
            repeat(10) { mockView.simulateUserMove(Direction.LEFT) }
        }

        almostCycleThroughLevel1()
        mockView.simulateUserMove(Direction.UP)
        mockView.useCurrentItem()

        almostCycleThroughLevel1()
        mockView.useCurrentItem()

        // sharpy killed, both items picked and used
        assertEquals(Experience(0, 1), controller.experience.getExperience(controller.hero))
        assertEquals(20, (controller.hero.units.first() as HpGameUnit).hp)
        assertEquals(RGB(1, 1, 1), controller.hero.units.first().gameColor)

        // go to next level
        mockView.simulateUserMove(Direction.UP)

        withTimeout(1000) {
            controller.awaitLoadedLevel("$filesFolder/level2.yaml")
        }

        repeat(20) {
            mockView.simulateUserMove(Direction.RIGHT) // move in order for sharpy to always attack
            delay(100)
            mockView.simulateUserMove(Direction.LEFT)
        }
        // hero killed by sharpy on 2nd level

        assertEquals(1, (controller.hero.units.first() as HpGameUnit).hp)
        assertEquals(1, (controller.hero.units.first() as HpGameUnit).maxHp)
        repeat(250) { // TODO: see bug in Hero fakePersistence
            mockView.simulateUserMove(Direction.RIGHT)
        }
        delay(100) // make sure coroutines stop
        assertFalse { controllerJob.isActive }
    }

    @Suppress("LongMethod")
    @RepeatedTest(5)
    fun testColors(): Unit = runBlocking {
        try {
            val mockView = MockView()
            controller = Controller(
                loadLevelLoader("$filesFolder/level3.yaml"),
                FileColorLoader("$filesFolder/colors.yaml"),
                FileExperienceLevelsLoader("$filesFolder/experience.yaml"),
                FileHeroLoader("$filesFolder/heroTough.yaml"),
                mockView
            )
            controller.receive(StartControllerMessage())
            val controllerJob = launch { controller.messagingRoutine() }

            withTimeout(2000) {
                controller.awaitLoadedLevel("$filesFolder/level3.yaml")
            }

            suspend fun spawnTestEntity(
                cell: Cell,
                color: RGB,
                attackType: AttackType = AttackType.HERO_TARGET
            ): Boolean {
                return controller.creation.tryAddToWorld(TestEntity(cell, color, 99999, attackType))
            }

            fun GameEntity.testColor(color: RGB) = units.first().gameColor == color
            val expectedEntitiesPredicates = mutableMapOf<(GameEntity) -> Boolean, Boolean>()
            for (c in 0..11) {
                val rgb = RGB(c, c, c)
                val cell = Cell(c + 1, c + 1)
                if (c == 11)
                    assertTrue { spawnTestEntity(cell, rgb) }
                else
                    assertTrue { spawnTestEntity(cell, rgb, AttackType.RANDOM_TARGET) }
                val p: (GameEntity) -> Boolean = {
                    it.testColor(rgb) && when (c) {
                        in 0..1 -> true
                        in 2..4 -> it is Fireball
                        in 5..7 -> it is WavePart
                        8 -> it is LaserPart
                        9 -> true
                        10 -> it is ConfuseBall
                        11 -> it is Icicle // only spawns with IcicleBomb
                        else -> unreachable
                    }
                }
                expectedEntitiesPredicates[p] = false
            }

            withTimeoutOrNull(10000) {
                while (true) {
                    val drawables = mockView.drawables
                    for (e in drawables.keys) {
                        for (p in expectedEntitiesPredicates.keys) {
                            if (p(e)) expectedEntitiesPredicates[p] = true
                        }
                    }
                    delay(1) // cancellation point
                }
            }

            withTimeout(30000) {
                controller.awaitLoadedLevel(controller.loseLevelDescriptionFilename)
            }
            // hero is dead by now

            assertTrue { expectedEntitiesPredicates.values.all { it } }

            repeat(200) { // TODO: (see previous test)
                mockView.simulateUserMove(Direction.RIGHT) // get to quit portal
            }
            delay(100)
            assertFalse { controllerJob.isActive }
        } catch (e: Throwable) {
            assertEquals("fail", e.stackTraceToString())
        } finally {
            assertEquals(null, exceptionStackTrace)
        }
    }

    private suspend fun Controller.awaitLoadedLevel(expectedLevelFilename: String) {
        while (currentLevelFilename != expectedLevelFilename ||
            stateRepresentation != Controller.ControllerStateRepresentation.PLAYING
        ) {
            delay(100)
        }
    }
}

private class MockView : View() {
    lateinit var lastInvSnapshot: InventoryViewSnapshot
    val drawables = DrawablesMap()

    inner class MockViewState : ViewState() {
        override fun next(m: Message): ViewState {
            when (m) {
                is InventoryOpened -> {
                    lastInvSnapshot = m.invSnapshot
                }
                is EntityUpdated -> {
                    drawables[m.gameEntity] = m.newSnapshot
                }
                is EntityRemoved -> {
                    drawables.remove(m.gameEntity)
                }
                is GameViewStopped -> {
                    drawables.clear()
                }
                else -> ignore
            }
            return this
        }
    }

    suspend fun simulateUserMove(dir: Direction) {
        controller.hero.receive(UserMoved(dir))
        delay(10)
    }

    suspend fun simulateUserToggleInv() {
        controller.hero.receive(UserToggledInventory())
        delay(10)
    }

    suspend fun simulateUserSelect() {
        controller.hero.receive(UserSelect())
        delay(10)
    }

    suspend fun useCurrentItem() {
        simulateUserToggleInv()
        simulateUserSelect()
        simulateUserToggleInv()
    }

    override val state: AtomicReference<ViewState> = AtomicReference(MockViewState())
}

class TestEntity(cell: Cell, color: RGB, hp: Int, attackType: AttackType) :
    GameEntity(setOf(ColorCellHp(color, cell, hp))) {
    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit) = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.CIRCLE, null)
        }
    }
    override val physicalEntity = object : PhysicalEntity() {
        override val isSolid = false
        override fun getUnitDirection(unit: GameUnit, dir: Direction) = dir
    }
    override val fightEntity = object : FightEntity() {
        override val teamId = 65
        override fun isUnitActive(unit: GameUnit) = true
    }
    override val experienceEntity = object : ExperienceEntity() {
        override val onDieExperiencePoints = 5
    }
    override val behaviourEntity = BehaviourEntity()
    override val lifecycle = BehaviourBuilder.lifecycle(this)
        .addBlocks {
            add { EnableColorUpdate(entity, childBlock, ControlParams(attackType, HealType.NO_HEAL)) }
        }
        .build()
}
