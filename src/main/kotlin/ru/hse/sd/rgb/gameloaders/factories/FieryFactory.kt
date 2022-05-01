package ru.hse.sd.rgb.gameloaders.factories

import ru.hse.sd.rgb.utils.RGB

class FieryFactory : LevelContentFactory() {

    override val bgColor = RGB(50, 30, 0)

    override val wallColor = RGB(170, 150, 130)
    override val wallHp = 999

}
