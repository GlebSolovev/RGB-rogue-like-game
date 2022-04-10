package ru.hse.sd.rgb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import ru.hse.sd.rgb.entities.Hero
import ru.hse.sd.rgb.logic.PhysicsLogic
import ru.hse.sd.rgb.views.SwingView
import ru.hse.sd.rgb.views.View
import java.util.concurrent.Executors


val viewCoroutineScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
val gameCoroutineScope =
    CoroutineScope(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).asCoroutineDispatcher())

interface Controller {
    val physics: PhysicsLogic
    val view: View
//    val hero: Hero

    fun start()
}

class ControllerImpl : Controller {
    // TODO: config (DI?)
    override val physics = PhysicsLogic(15, 20)
    override val view = SwingView(0, 0, 800, 600, 40)

    private val hero = Hero()

    override fun start() {
        viewCoroutineScope.launch {
            view.initialize()
            view.messagingRoutine()
        }
        gameCoroutineScope.launch {
            view.receive(View.SubscribeToMovement(hero))
            hero.messagingRoutine()
        }
    }
}

var controller: Controller = ControllerImpl()

fun main() {
    controller.start()
}
