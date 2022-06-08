package ru.hse.sd.rgb

import ru.hse.sd.rgb.gameloaders.FileColorLoader
import ru.hse.sd.rgb.gameloaders.FileExperienceLevelsLoader
import ru.hse.sd.rgb.gameloaders.FileHeroLoader
import ru.hse.sd.rgb.gameloaders.FileLevelLoader
import ru.hse.sd.rgb.gamelogic.Controller
import ru.hse.sd.rgb.gamelogic.engines.experience.Experience
import ru.hse.sd.rgb.gamelogic.engines.items.InventoryViewSnapshot
import ru.hse.sd.rgb.gamelogic.entities.HpGameUnit
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Hero
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Sharpy
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Wall
import ru.hse.sd.rgb.utils.ignore
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.messages.*
import ru.hse.sd.rgb.utils.structures.Direction
import ru.hse.sd.rgb.utils.structures.Grid2D
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.views.DrawablesMap
import ru.hse.sd.rgb.views.View
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class IntegrationTest {

    private val filesFolder = "src/test/resources/integration"

    @Test
    fun test(): Unit = runBlocking {
        val mockView = MockView()
        controller = Controller(
            FileLevelLoader("$filesFolder/level1.yaml"),
            FileColorLoader("$filesFolder/colors.yaml"),
            FileExperienceLevelsLoader("$filesFolder/experience.yaml"),
            FileHeroLoader("$filesFolder/hero.yaml"),
            mockView
        )
        controller.receive(StartControllerMessage())
        val controllerJob = launch { controller.messagingRoutine() }
        delay(1000) // initial loading takes unknown time

        // useful for debugging
        fun hehe() {
            val tmp = mockView.drawables
            val rep = Grid2D(50, 30) { _, _ -> '.' }
            for ((e, snap) in tmp) {
                for (a in snap) {
                    rep[a.cell] = when (e) {
                        is Wall -> '#'
                        is Hero -> 'H'
                        is Sharpy -> 'S'
                        else -> '.'
                    }
                }
            }
            println(rep)
        }

        suspend fun cycleThroughLevel1() {
            repeat(10) { mockView.simulateUserMove(Direction.RIGHT) }
            mockView.simulateUserMove(Direction.DOWN)
            repeat(10) { mockView.simulateUserMove(Direction.LEFT) }
            mockView.simulateUserMove(Direction.UP)
        }

        repeat(2) {
            cycleThroughLevel1()
            mockView.useCurrentItem()
        }

        // sharpy killed, both items picked and used, already on next level
        assertEquals(
            Experience(0, 1),
            controller.experience.getExperience(controller.hero)
        )
        assertEquals(
            20,
            (controller.hero.units.first() as HpGameUnit).hp
        )
        assertEquals(
            RGB(1, 1, 1),
            controller.hero.units.first().gameColor
        )

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
        assertFalse { controllerJob.isActive }
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

    suspend fun simulateUserDrop() {
        controller.hero.receive(UserDrop())
        delay(10)
    }

    override val state: AtomicReference<ViewState> = AtomicReference(MockViewState())
}
