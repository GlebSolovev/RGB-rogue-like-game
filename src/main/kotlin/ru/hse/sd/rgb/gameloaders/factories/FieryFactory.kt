package ru.hse.sd.rgb.gameloaders.factories

import ru.hse.sd.rgb.utils.RGB

class FieryFactory : LevelContentFactory() {

    override val bgColor = RGB(70, 20, 0)

    override val wallColor = RGB(200, 110, 60)

    override val glitchSpawnRate = 1.0 / (40 * 40)
    override val glitchHp = 1

}
