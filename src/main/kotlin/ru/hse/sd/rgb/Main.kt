package ru.hse.sd.rgb

import kotlinx.coroutines.runBlocking
import ru.hse.sd.rgb.gameloaders.FileColorLoader
import ru.hse.sd.rgb.gameloaders.InventoryDescription
import ru.hse.sd.rgb.gameloaders.RandomLevelLoader
import ru.hse.sd.rgb.gameloaders.factories.FieryFactory
import ru.hse.sd.rgb.gamelogic.Controller
import ru.hse.sd.rgb.gamelogic.onException
import ru.hse.sd.rgb.utils.messaging.messages.StartControllerMessage
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.views.swing.SwingView

//private val levelFilename: String? = null // "src/main/resources/sampleLevel.description" // TODO: CLI argument
private const val colorsFilename: String = "src/main/resources/gameColors.yaml"

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
    FileColorLoader(colorsFilename),
    SwingView()
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
