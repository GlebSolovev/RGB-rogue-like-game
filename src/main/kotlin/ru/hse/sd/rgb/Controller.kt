package ru.hse.sd.rgb

import kotlinx.coroutines.*
import ru.hse.sd.rgb.entities.Hero
import ru.hse.sd.rgb.entities.common.GameStarted
import ru.hse.sd.rgb.levelloading.loadLevel
import ru.hse.sd.rgb.logic.CreationLogic
import ru.hse.sd.rgb.logic.FightLogic
import ru.hse.sd.rgb.logic.PhysicsLogic
import ru.hse.sd.rgb.views.GameViewStarted
import ru.hse.sd.rgb.views.UserQuit
import ru.hse.sd.rgb.views.View
import ru.hse.sd.rgb.views.swing.SwingView
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

val viewCoroutineScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
val gameCoroutineScope =
    CoroutineScope(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).asCoroutineDispatcher())

class Controller : Messagable() {

    private var stateRef: AtomicReference<ControllerState> = AtomicReference(GameInitState())

    override suspend fun handleMessage(m: Message) {
        stateRef.get().next(m)
    }

    private abstract class ControllerState {
        abstract suspend fun next(m: Message)
    }

    private inner class GameInitState : ControllerState() {
        override suspend fun next(m: Message): Unit = when (m) {
            is StartControllerMessage -> {
                start()
            }
            is UserQuit -> {
                quit()
            }
            else -> unreachable
        }
    }

    private inner class GamePlayingState(
        val creation: CreationLogic,
        val physics: PhysicsLogic,
        val fighting: FightLogic,
        val view: View,
        val hero: Hero,
    ) : ControllerState() {
        override suspend fun next(m: Message): Unit = when (m) {
            is FinishControllerMessage, is UserQuit -> { // TODO: finish screen
                quit()
            }
            else -> unreachable
        }
    }

    private suspend fun start() {
        val view = SwingView()
        viewCoroutineScope.launch {
            view.initialize()
            view.messagingRoutine()
        }
        view.receive(View.SubscribeToQuit(this@Controller))

        val level = loadLevel(filename)
//        delay(1.seconds)

        val physics = PhysicsLogic(level.h, level.w)
        val fighting = FightLogic()
        val creation = CreationLogic(physics)
        val hero = level.hero

        for (entity in level.allEntities) {
            if (!creation.tryAddToWorld(entity)) throw IllegalStateException("invalid level")
        }

        stateRef.set(GamePlayingState(creation, physics, fighting, view, hero))

        view.receive(GameViewStarted(level))
        for (entity in level.allEntities) {
            gameCoroutineScope.launch { entity.messagingRoutine() }
            entity.receive(GameStarted())
        }
    }

    private fun quit(): Nothing {
        gameCoroutineScope.cancel()
        Ticker.resetAll()
        exitProcess(0)
    }

    val creation: CreationLogic
        get() {
            val state = stateRef.get()
            return if (state is GamePlayingState) state.creation else throw IllegalStateException()
        }

    val physics: PhysicsLogic
        get() {
            val state = stateRef.get()
            return if (state is GamePlayingState) state.physics else throw IllegalStateException()
        }

    val fighting: FightLogic
        get() {
            val state = stateRef.get()
            return if (state is GamePlayingState) state.fighting else throw IllegalStateException()
        }

    val view: View // TODO: should be global for Controller
        get() {
            val state = stateRef.get()
            return if (state is GamePlayingState) state.view else throw IllegalStateException()
        }

    val hero: Hero
        get() {
            val state = stateRef.get()
            return if (state is GamePlayingState) state.hero else throw IllegalStateException()
        }
}

var controller = Controller()
val filename: String? = null // "src/test/resources/sampleLevel.description" // TODO: CLI argument

class StartControllerMessage : Message()
data class FinishControllerMessage(val isWin: Boolean) : Message()

fun main() = runBlocking {
    controller.receive(StartControllerMessage())
    controller.messagingRoutine()
}
