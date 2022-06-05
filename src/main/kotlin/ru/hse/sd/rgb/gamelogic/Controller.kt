package ru.hse.sd.rgb.gamelogic

import kotlinx.coroutines.*
import ru.hse.sd.rgb.gameloaders.*
import ru.hse.sd.rgb.gameloaders.factories.FieryFactory
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Hero
import ru.hse.sd.rgb.utils.messaging.Messagable
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.*
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.utils.unreachable
import ru.hse.sd.rgb.views.View
import ru.hse.sd.rgb.views.swing.SwingView
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

val exceptionHandler = CoroutineExceptionHandler { _, e ->
    e.printStackTrace()
    onException()
}

val viewCoroutineScope = CoroutineScope(
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()
            + exceptionHandler
)
val gameCoroutineScope = CoroutineScope(
    Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).asCoroutineDispatcher()
            + exceptionHandler
)

fun onException() {
    gameCoroutineScope.cancel()
    viewCoroutineScope.cancel()
    exitProcess(5)
}

val levelFilename: String? = null // "src/main/resources/sampleLevel.description" // TODO: CLI argument
const val colorsFilename: String = "src/main/resources/gameColors.yaml"

class Controller(val view: View) : Messagable() {

    private var stateRef: AtomicReference<ControllerState> = AtomicReference(GameInitState())

    override suspend fun handleMessage(m: Message) {
        stateRef.get().next(m)
    }

    private abstract class ControllerState {
        abstract suspend fun next(m: Message)

        open val engines: Engines
            get() = throw IllegalStateException("no engines in this state")

        open val hero: Hero
            get() = throw IllegalStateException("no hero will save you")
    }

    private inner class GameInitState : ControllerState() {

        val levelLoader = RandomLevelLoader.builder {
            width = 70
            height = random(50..60)
            chamberMinSize = 10
            heroInventory = InventoryDescription(3, 4)
            factory = FieryFactory()
            heroColor = RGB(200, 60, 200)
        }
        val colorLoader = FileColorLoader(colorsFilename)

        val loader = Loader(levelLoader, colorLoader)

        override lateinit var engines: Engines
        override lateinit var hero: Hero

        override suspend fun next(m: Message): Unit = when (m) {
            is StartControllerMessage -> {
                startGame()
            }
            is UserQuit -> {
                quit()
            }
            else -> unreachable
        }

        private suspend fun startGame() {
            viewCoroutineScope.launch {
                view.initialize()
                view.messagingRoutine()
            }
            view.receive(SubscribeToQuit(this@Controller))
            // delay(700) // helps to see loading screen

            // load engines before loading entities
            engines = loader.loadEngines()

            hero = loader.loadHero() // invariant: hero always exists

            val level = loader.loadLevel()
            val (gameDesc, _) = level

            creation.addAllToWorld(gameDesc.allEntities) {
                view.receive(GameViewStarted(level))
            }

            stateRef.set(GamePlayingState(engines, hero))
        }
    }

    private inner class GamePlayingState(
        override val engines: Engines,
        override val hero: Hero
    ) : ControllerState() {
        override suspend fun next(m: Message): Unit = when (m) {
            is FinishControllerMessage, is UserQuit -> { // TODO: finish screen
                quit()
            }
            else -> unreachable
        }
    }

    private fun quit(): Nothing {
        gameCoroutineScope.cancel()
        Ticker.resetAll()
        exitProcess(0)
    }

    val physics
        get() = stateRef.get().engines.physics
    val fighting
        get() = stateRef.get().engines.fighting
    val creation
        get() = stateRef.get().engines.creation
    val behaviour
        get() = stateRef.get().engines.behaviour

    val hero: Hero
        get() = stateRef.get().hero
}

var controller = Controller(SwingView()) // TODO: DI

fun main() = runBlocking {
    controller.receive(StartControllerMessage())
    try {
        controller.messagingRoutine()
    } catch (t: Throwable) {
        t.printStackTrace()
        onException()
    }
}
