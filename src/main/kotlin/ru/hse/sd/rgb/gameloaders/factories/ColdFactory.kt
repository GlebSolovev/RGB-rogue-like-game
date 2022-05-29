package ru.hse.sd.rgb.gameloaders.factories

import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Sharpy
import ru.hse.sd.rgb.utils.structures.RGB

class ColdFactory : LevelContentFactory() {

    override val bgColor = RGB(0, 10, 50)

    override val wallColor: RGB = RGB(100, 120, 200)

    override val glitchSpawnRate = 1.0 / (60 * 60)
    override val glitchHp = 1

    override val sharpySpawnCount: Int = 10
    override val sharpyColor = RGB(50, 50, 70)
    override val sharpyHp: Int = 6
    override val sharpyMovePeriodMillis: Long = 1100
    override val sharpySeeingDepth: Int = 10
    override val sharpyWatchPeriodMillis: Long =
        (sharpyMovePeriodMillis / Sharpy.DIRECT_ATTACK_MOVE_PERIOD_COEFFICIENT).toLong() - 30

}
