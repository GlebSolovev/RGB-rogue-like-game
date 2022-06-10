@file:Suppress("TooGenericExceptionCaught", "MagicNumber")

package ru.hse.sd.rgb

import ru.hse.sd.rgb.gameloaders.FileColorLoader
import ru.hse.sd.rgb.gameloaders.FileExperienceLevelsLoader
import ru.hse.sd.rgb.gameloaders.FileHeroLoader
import ru.hse.sd.rgb.gameloaders.FileLevelLoader
import ru.hse.sd.rgb.gamelogic.Controller
import ru.hse.sd.rgb.gamelogic.onException
import ru.hse.sd.rgb.utils.messaging.messages.StartControllerMessage
import ru.hse.sd.rgb.views.swing.SwingView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// TODO: CLI argument
private const val LEVEL_FILENAME: String = "src/main/resources/sampleLevel.yaml"
private const val COLORS_FILENAME: String = "src/main/resources/gameColors.yaml"
private const val HERO_EXPERIENCE_LEVELS_FILENAME: String = "src/main/resources/heroExperienceLevels.yaml"
private const val HERO_FILENAME: String = "src/main/resources/sampleHero.yaml"

private val levelLoader = FileLevelLoader(LEVEL_FILENAME)

var controller = Controller(
    levelLoader,
    FileColorLoader(COLORS_FILENAME),
    FileExperienceLevelsLoader(HERO_EXPERIENCE_LEVELS_FILENAME),
    FileHeroLoader(HERO_FILENAME),
    SwingView(10)
) // TODO: DI

fun main() = runBlocking {
    controller.receive(StartControllerMessage())
    launch {
        try {
            controller.messagingRoutine()
        } catch (_: CancellationException) {
        } catch (t: Throwable) {
            t.printStackTrace()
            onException(t)
        }
    }.join()
}
