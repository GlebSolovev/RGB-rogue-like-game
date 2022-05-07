package ru.hse.sd.rgb.gameloaders.factories

import ru.hse.sd.rgb.utils.RGB

class ColdFactory: LevelContentFactory() {
    override val bgColor = RGB(0, 10, 50)

    override val wallColor: RGB = RGB(100, 120, 200)

    override val glitchSpawnRate = 1.0 / (60 * 60)
    override val glitchHp = 1

}
