@file:Suppress("MagicNumber")

package ru.hse.sd.rgb.gameloaders.factories

import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.utils.structures.RGBDelta
import kotlin.random.Random

class FieryFactory(random: Random = Random) : LevelContentFactory() {

    override val bgColor = RGB(70, 20, 0)

    override val wallColor = RGB(200, 110, 60)

    override val glitchSpawnCount = random.nextInt(0, 3)
    override val glitchHp = 1
    override val glitchClonePeriod = 4000L

    override val sharpySpawnCount: Int = random.nextInt(3, 6)
    override val sharpyColor = RGB(70, 50, 50)
    override val sharpyHp: Int = 4
    override val sharpyMovePeriodMillis: Long = 900
    override val sharpySeeingDepth: Int = 20

    override val colorModificationSpawnCount = random.nextInt(5, 15)
    override val colorModificationRGBDeltaGenerationTable = GenerationTable.builder<RGBDelta>()
        .outcome(5) { RGBDelta(20, 0, 0) }
        .outcome(5) { RGBDelta(10, 5, 5) }
        .outcome(1) { RGBDelta(50, 0, 0) }
        .outcome(4) { RGBDelta(30, -10, -10) }
        .addDefaults(2)
        .build()

    override val instantHealSpawnCount = random.nextInt(1, 6)
    override val instantHealGenerationTable = GenerationTable.builder<Int>()
        .outcome(1) { 4 }
        .build()
}
