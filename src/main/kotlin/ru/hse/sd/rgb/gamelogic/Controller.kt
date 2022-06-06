package ru.hse.sd.rgb.gamelogic

import kotlinx.coroutines.*
import ru.hse.sd.rgb.gameloaders.ColorLoader
import ru.hse.sd.rgb.gameloaders.Engines
import ru.hse.sd.rgb.gameloaders.LevelLoader
import ru.hse.sd.rgb.gameloaders.Loader
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Hero
import ru.hse.sd.rgb.utils.messaging.Messagable
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.*
import ru.hse.sd.rgb.utils.unreachable
import ru.hse.sd.rgb.views.View
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

private val exceptionHandler = CoroutineExceptionHandler { _, e ->
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

class Controller(
    private val levelLoader: LevelLoader,
    private val colorLoader: ColorLoader,
    val view: View
    ) : Messagable() {

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
    val behaviourEngine
        get() = stateRef.get().engines.behaviour

    val hero: Hero
        get() = stateRef.get().hero
}
