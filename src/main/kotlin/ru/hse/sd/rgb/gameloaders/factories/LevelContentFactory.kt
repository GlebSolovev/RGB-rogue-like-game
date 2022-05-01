package ru.hse.sd.rgb.gameloaders.factories

import ru.hse.sd.rgb.gamelogic.engines.fight.GameColor
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Wall
import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.utils.RGB

abstract class LevelContentFactory {

    abstract val bgColor: RGB

    protected abstract val wallColor: RGB
    protected abstract val wallHp: Int
    open fun createWall(cell: Cell): Wall = Wall(GameColor(wallColor), wallHp, cell)

}
