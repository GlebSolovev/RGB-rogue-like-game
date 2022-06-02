package ru.hse.sd.rgb.gameloaders.factories

import ru.hse.sd.rgb.utils.structures.RGB

class FieryFactory : LevelContentFactory() {

    override val bgColor = RGB(70, 20, 0)

    override val wallColor = RGB(200, 110, 60)

    override val glitchSpawnRate = 1.0 / (40 * 40)
    override val glitchHp = 1
    override val glitchClonePeriod = 4000L

    override val sharpySpawnCount: Int = 4
    override val sharpyColor = RGB(70, 50, 50)
    override val sharpyHp: Int = 4
    override val sharpyMovePeriodMillis: Long = 900
    override val sharpySeeingDepth: Int = 20
}
