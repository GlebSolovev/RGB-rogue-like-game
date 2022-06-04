package ru.hse.sd.rgb.gameloaders.factories

import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.utils.structures.RGBDelta

class ColdFactory : LevelContentFactory() {

    override val bgColor = RGB(0, 10, 50)

    override val wallColor: RGB = RGB(100, 120, 200)

    override val glitchSpawnRate = 1.0 / (60 * 60)
    override val glitchHp = 1
    override val glitchClonePeriod = 5000L

    override val sharpySpawnCount: Int = 10
    override val sharpyColor = RGB(50, 50, 70)
    override val sharpyHp: Int = 6
    override val sharpyMovePeriodMillis: Long = 1100
    override val sharpySeeingDepth: Int = 10

    override val colorModificationSpawnRate = 1.0 / (20 * 20)
    override val colorModificationRGBDeltaGenerationTable = GenerationTable.builder<RGBDelta>()
        .outcome(7) { RGBDelta(0, 0, 15) }
        .outcome(7) { RGBDelta(0, 10, 10) }
        .outcome(7) { RGBDelta(10, -10, 30) }
        .outcome(3) { RGBDelta(-20, 20, 40) }
        .outcome(1) { RGBDelta(-50, 30, 50) }
        .build()
}
