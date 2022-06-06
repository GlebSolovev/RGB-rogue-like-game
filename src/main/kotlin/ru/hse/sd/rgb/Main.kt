@file:Suppress("TooGenericExceptionCaught", "MagicNumber")

package ru.hse.sd.rgb

import ru.hse.sd.rgb.gameloaders.FileColorLoader
import ru.hse.sd.rgb.gameloaders.InventoryDescription
import ru.hse.sd.rgb.gameloaders.RandomLevelLoader
import ru.hse.sd.rgb.gameloaders.factories.FieryFactory
import ru.hse.sd.rgb.gamelogic.Controller
import ru.hse.sd.rgb.gamelogic.onException
import ru.hse.sd.rgb.utils.messaging.messages.StartControllerMessage
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.views.swing.SwingView
import kotlinx.coroutines.runBlocking

// private val levelFilename: String? = null // "src/main/resources/sampleLevel.description" // TODO: CLI argument
private const val COLORS_FILENAME: String = "src/main/resources/gameColors.yaml"

private val levelLoader = RandomLevelLoader.builder {
    width = 70
    height = random(50..60)
    chamberMinSize = 10
    heroInventory = InventoryDescription(3, 4)
    factory = FieryFactory()
    heroColor = RGB(200, 60, 200)
    heroHp = 1000
}

val controller = Controller(
    levelLoader,
    FileColorLoader(COLORS_FILENAME),
    SwingView(10)
) // TODO: DI

fun main() = runBlocking {
    controller.receive(StartControllerMessage())
    try {
        controller.messagingRoutine()
    } catch (t: Throwable) {
        t.printStackTrace()
        onException()
    }
}
