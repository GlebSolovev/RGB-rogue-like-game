package ru.hse.sd.rgb.gamelogic

import kotlinx.coroutines.*
import ru.hse.sd.rgb.gameloaders.Engines
import ru.hse.sd.rgb.gameloaders.Loader
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Hero
import ru.hse.sd.rgb.gamelogic.entities.GameStarted
import ru.hse.sd.rgb.utils.Messagable
import ru.hse.sd.rgb.utils.Message
import ru.hse.sd.rgb.utils.Ticker
import ru.hse.sd.rgb.utils.unreachable
import ru.hse.sd.rgb.views.GameViewStarted
import ru.hse.sd.rgb.views.UserQuit
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
        val loader = Loader(levelFilename, colorsFilename)

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
            view.receive(View.SubscribeToQuit(this@Controller))
//            delay(1.seconds) // helps to see loading screen

            // load engines before loading entities
            engines = loader.loadEngines()

            val level = loader.loadLevel()
            val (gameDesc, _) = level
            hero = gameDesc.hero
            for (entity in gameDesc.allEntities) {
                if (!creation.tryAddToWorld(entity)) throw IllegalStateException("invalid level")
            }

            view.receive(GameViewStarted(level))
            for (entity in gameDesc.allEntities) {
                gameCoroutineScope.launch { entity.messagingRoutine() }
                entity.receive(GameStarted())
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

    val hero: Hero
        get() = stateRef.get().hero
}

var controller = Controller(SwingView()) // TODO: DI

class StartControllerMessage : Message()
data class FinishControllerMessage(val isWin: Boolean) : Message()

fun main() = runBlocking {
    controller.receive(StartControllerMessage())
    try {
        controller.messagingRoutine()
    } catch (t: Throwable) {
        t.printStackTrace()
        onException()
    }
}
