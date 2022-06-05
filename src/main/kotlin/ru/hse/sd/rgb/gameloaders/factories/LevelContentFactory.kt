package ru.hse.sd.rgb.gameloaders.factories

import ru.hse.sd.rgb.controller
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

    abstract val instantHealSpawnRate: Double
    abstract val instantHealGenerationTable: GenerationTable<Int>

    abstract val wallColor: RGB
    open fun createWall(cell: Cell): Wall = Wall(wallColor, cell)

    abstract val glitchHp: Int
    abstract val glitchClonePeriod: Long
    open fun createGlitch(cell: Cell): Glitch =
        Glitch(cell, glitchHp, glitchClonePeriod, controller.fighting.newTeamId())

    abstract val sharpyColor: RGB
    abstract val sharpyHp: Int
    abstract val sharpyMovePeriodMillis: Long
    abstract val sharpySeeingDepth: Int
    open fun createSharpy(cell: Cell): Sharpy = Sharpy(
        ColorCellHp(sharpyColor, cell, sharpyHp),
        sharpyMovePeriodMillis,
        sharpySeeingDepth,
        controller.fighting.newTeamId()
    )
}
