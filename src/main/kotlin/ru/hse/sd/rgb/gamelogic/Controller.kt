@file:Suppress("WildcardImport")

package ru.hse.sd.rgb.gamelogic

import ru.hse.sd.rgb.gameloaders.*
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourEngine
import ru.hse.sd.rgb.gamelogic.engines.creation.CreationEngine
import ru.hse.sd.rgb.gamelogic.engines.fight.FightEngine
import ru.hse.sd.rgb.gamelogic.engines.items.ItemsEngine
import ru.hse.sd.rgb.gamelogic.engines.physics.PhysicsEngine
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Hero
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.HeroPersistence
import ru.hse.sd.rgb.utils.getValue
import ru.hse.sd.rgb.utils.messaging.Messagable
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.*
import ru.hse.sd.rgb.utils.setValue
import ru.hse.sd.rgb.utils.unreachable
import ru.hse.sd.rgb.views.View
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

private val exceptionHandler = CoroutineExceptionHandler { _, e ->
    e.printStackTrace()
    onException()
}

private fun createGameCoroutineScope() = CoroutineScope(
    Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).asCoroutineDispatcher() +
        exceptionHandler
)

val viewCoroutineScope = CoroutineScope(
    Executors.newSingleThreadExecutor().asCoroutineDispatcher() +
        exceptionHandler
)
var gameCoroutineScope = createGameCoroutineScope()

const val ON_EXCEPTION_EXIT_CODE = 5

fun onException() {
    gameCoroutineScope.cancel()
    viewCoroutineScope.cancel()
    exitProcess(ON_EXCEPTION_EXIT_CODE)
}

class Controller(
    initialLevelLoader: LevelLoader,
    private val colorLoader: ColorLoader, // TODO: changing base colors between levels is absurd, but can be added
    heroLoader: HeroLoader,
    val view: View
) : Messagable() {

    private var state: ControllerState by AtomicReference<ControllerState>(
        GameInitState(
            initialLevelLoader,
            colorLoader,
            heroLoader.loadHeroInitParams().convertToInitialHeroPersistence()
        )
    )

    override suspend fun handleMessage(m: Message) {
        state = state.next(m)
    }

    private abstract class ControllerState {
        abstract suspend fun next(m: Message): ControllerState

        open val engines: Engines
            get() = throw IllegalStateException("no engines in this state")

        open val hero: Hero
            get() = throw IllegalStateException("no hero will save you")
    }

    private inner class GameInitState(
        levelLoader: LevelLoader,
        colorLoader: ColorLoader,
        private val heroPersistence: HeroPersistence,
    ) : ControllerState() {
        val gameLoader = GameLoader(levelLoader, colorLoader)

        override lateinit var hero: Hero
        override lateinit var engines: Engines

        override suspend fun next(m: Message) = when (m) {
            is StartControllerMessage -> {
                startGame()
            }
            is DoLoadLevel -> {
                loadLevel()
            }
            is UserQuit -> {
                quit()
            }
            else -> unreachable
        }

        private suspend fun startGame(): GamePlayingState {
            viewCoroutineScope.launch {
                view.initialize()
                view.messagingRoutine()
            }
            view.receive(SubscribeToQuit(this@Controller))

            return loadLevel()
        }

        private suspend fun loadLevel(): GamePlayingState {
            engines = gameLoader.loadEngines() // load engines before loading entities
            hero = gameLoader.populateHero(heroPersistence) // invariant: hero always exists

            val level = gameLoader.loadLevel()
            val (gameDesc, _) = level

            creation.addAllToWorld(gameDesc.allEntities) {
                view.receive(GameViewStarted(level))
            }
            return GamePlayingState(engines, hero)
        }
    }

    private inner class GamePlayingState(
        override val engines: Engines,
        override val hero: Hero
    ) : ControllerState() {
        override suspend fun next(m: Message) = when (m) {
            is FinishControllerMessage, is UserQuit -> { // TODO: finish screen
                quit()
            }
            is NextLevel -> {
                val heroPersistence = hero.extractPersistence() // TODO: not thread-safe
                stopGame()
                view.receive(GameViewStopped())
                val nextLevelLoader = FileLevelLoader(m.nextLevelDescriptionFilename)
                receive(DoLoadLevel())
                GameInitState(nextLevelLoader, colorLoader, heroPersistence)
            }
            else -> unreachable
        }
    }

    private fun stopGame() {
        gameCoroutineScope.cancel()
        gameCoroutineScope = createGameCoroutineScope()
        Ticker.stopDefaultScope()
    }

    private fun quit(): Nothing {
        stopGame()
        exitProcess(0)
    }

    val physics: PhysicsEngine get() = state.engines.physics
    val fighting: FightEngine get() = state.engines.fighting
    val creation: CreationEngine get() = state.engines.creation
    val behaviourEngine: BehaviourEngine get() = state.engines.behaviourEngine
    val itemsEngine: ItemsEngine get() = state.engines.itemsEngine

    val hero: Hero get() = state.hero
}
