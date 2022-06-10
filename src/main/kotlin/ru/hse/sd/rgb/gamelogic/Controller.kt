@file:Suppress("WildcardImport")

package ru.hse.sd.rgb.gamelogic

import ru.hse.sd.rgb.gameloaders.*
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourEngine
import ru.hse.sd.rgb.gamelogic.engines.creation.CreationEngine
import ru.hse.sd.rgb.gamelogic.engines.experience.ExperienceEngine
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

var exceptionStackTrace: String? by AtomicReference(null)

fun onException() {
    gameCoroutineScope.cancel()
    viewCoroutineScope.cancel()
    exitProcess(ON_EXCEPTION_EXIT_CODE)
}

@Suppress("LongParameterList")
class Controller(
    initialLevelLoader: LevelLoader,
    private val colorLoader: ColorLoader, // TODO: changing base colors between levels is absurd, but can be added
    private val experienceLevelsLoader: ExperienceLevelsLoader,
    heroLoader: HeroLoader,
    val view: View,
    val winLevelDescriptionFilename: String = "src/main/resources/ending/levelWin.yaml",
    val loseLevelDescriptionFilename: String = "src/main/resources/ending/levelLose.yaml"
) : Messagable() {

    companion object {
        private const val QUIT_LEVEL_FILENAME_ALIAS = "quit"
    }

    val stateRepresentation: ControllerStateRepresentation
        get() = when (state) {
            is GameInitState -> ControllerStateRepresentation.INIT
            is GamePlayingState -> ControllerStateRepresentation.PLAYING
            else -> unreachable
        }

    var currentLevelFilename: String? by AtomicReference<String?>(null)

    private var state: ControllerState by AtomicReference<ControllerState>(
        GameInitState(
            initialLevelLoader,
            colorLoader,
            experienceLevelsLoader,
            heroLoader.loadHeroInitParams().convertToInitialHeroPersistence()
        )
    )

    enum class ControllerStateRepresentation {
        INIT, PLAYING
    }

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
        experienceLevelsLoader: ExperienceLevelsLoader,
        private val heroPersistence: HeroPersistence,
    ) : ControllerState() {
        init {
            currentLevelFilename = (levelLoader as? FileLevelLoader)?.filename
        }

        val gameLoader = GameLoader(levelLoader, colorLoader, experienceLevelsLoader)

        override lateinit var hero: Hero
        override lateinit var engines: Engines

        override suspend fun next(m: Message) = when (m) {
            is StartControllerMessage -> startGame()
            is DoLoadLevel -> loadLevel()
            is UserQuit -> quit()
            else -> {
                println(m)
                unreachable
            }
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
            is UserQuit -> quit()
            is ControllerNextLevel -> {
                val heroPersistence = m.heroPersistence
                stopGame()
                view.receive(GameViewStopped())
                val nextLevelFilename = m.nextLevelDescriptionFilename
                if (nextLevelFilename != QUIT_LEVEL_FILENAME_ALIAS) {
                    val nextLevelLoader = FileLevelLoader(nextLevelFilename)
                    receive(DoLoadLevel())
                    GameInitState(nextLevelLoader, colorLoader, experienceLevelsLoader, heroPersistence)
                } else {
                    quit()
                }
            }
            else -> unreachable
        }
    }

    private suspend fun stopGame() {
        creation.removeAllAndJoin()
        gameCoroutineScope.cancel()
        Ticker.stopDefaultScope()

        gameCoroutineScope = createGameCoroutineScope()
    }

    private suspend fun quit(): Nothing {
        stopGame()
        // view is responsible for stopping itself
        view.receive(QuitView())
        throw CancellationException()
    }

    val physics: PhysicsEngine get() = state.engines.physics
    val fighting: FightEngine get() = state.engines.fighting
    val creation: CreationEngine get() = state.engines.creation
    val behaviourEngine: BehaviourEngine get() = state.engines.behaviourEngine
    val itemsEngine: ItemsEngine get() = state.engines.itemsEngine
    val experience: ExperienceEngine get() = state.engines.experience

    val hero: Hero get() = state.hero
}
