package ru.hse.sd.rgb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.hse.sd.rgb.entities.Hero
import ru.hse.sd.rgb.entities.Wall
import ru.hse.sd.rgb.logic.PhysicsLogic
import ru.hse.sd.rgb.logic.generateMaze
import ru.hse.sd.rgb.views.EntityMoved
import ru.hse.sd.rgb.views.swing.SwingView
import ru.hse.sd.rgb.views.View
import java.util.concurrent.Executors

val viewCoroutineScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
val gameCoroutineScope =
    CoroutineScope(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).asCoroutineDispatcher())

interface Controller {
    val physics: PhysicsLogic
    val view: View
//    val hero: Hero

    suspend fun start()
}

class ControllerImpl : Controller {
    // TODO: config (DI?)
    val h = 40
    val w = 80
    override val physics = PhysicsLogic(h, w)
    override val view = SwingView(0, 0, 1600, 800, 10)

    private val hero = Hero()

    override suspend fun start() {
        viewCoroutineScope.launch {
            view.initialize()
            view.messagingRoutine()
        }

        val level = generateMaze(w, h, 3, 3)
        for(x in 0 until w) {
            for(y in 0 until h) {
                if (level[y][x]) {
                    val wall = Wall(x, y) // TODO: consistency with i,j ordering
                    physics.tryPopulate(wall)
                    view.receive(EntityMoved(wall, wall.viewEntity.takeViewSnapshot())) // TODO: too decoupled?
                    // TODO: only pass entity to EntityMoved
                }
            }
        }

        gameCoroutineScope.launch {
            view.receive(View.SubscribeToMovement(hero))
            hero.messagingRoutine()
        }
    }
}

var controller: Controller = ControllerImpl()

fun main() = runBlocking {
    controller.start()
}
