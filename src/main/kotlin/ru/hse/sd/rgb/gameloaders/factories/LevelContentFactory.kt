package ru.hse.sd.rgb.gameloaders.factories

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.entities.ColorCellHp
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Glitch
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Sharpy
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Wall
import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.utils.structures.RGBDelta

abstract class LevelContentFactory {

    abstract val bgColor: RGB
    abstract val glitchSpawnRate: Double
    abstract val sharpySpawnCount: Int
    abstract val colorModificationSpawnRate: Double
    abstract val colorModificationRGBDeltaGenerationTable: GenerationTable<RGBDelta>

    protected abstract val wallColor: RGB
    open fun createWall(cell: Cell): Wall = Wall(wallColor, cell)

    protected abstract val glitchHp: Int
    protected abstract val glitchClonePeriod: Long
    open fun createGlitch(cell: Cell): Glitch =
        Glitch(cell, glitchHp, glitchClonePeriod, controller.fighting.newTeamId())

    protected abstract val sharpyColor: RGB
    protected abstract val sharpyHp: Int
    protected abstract val sharpyMovePeriodMillis: Long
    protected abstract val sharpySeeingDepth: Int
    open fun createSharpy(cell: Cell): Sharpy = Sharpy(
        ColorCellHp(sharpyColor, cell, sharpyHp),
        sharpyMovePeriodMillis,
        sharpySeeingDepth,
        controller.fighting.newTeamId()
    )

}
