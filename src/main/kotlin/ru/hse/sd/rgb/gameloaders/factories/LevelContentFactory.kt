package ru.hse.sd.rgb.gameloaders.factories

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.entities.ColorCellHp
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Glitch
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Sharpy
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Wall
import ru.hse.sd.rgb.gamelogic.items.scriptitems.ColorModificationEntity
import ru.hse.sd.rgb.gamelogic.items.scriptitems.InstantHealEntity
import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.utils.structures.RGBDelta

abstract class LevelContentFactory {

    abstract val bgColor: RGB

    abstract val wallColor: RGB
    open fun createWall(cell: Cell): Wall = Wall(wallColor, cell)

    // TODO: discuss count vs rate
    // count advantage: fixed and certain range of values
    // rate advantage: implicitly takes level size into account
    abstract val glitchSpawnCount: Int
    abstract val glitchHp: Int
    abstract val glitchClonePeriod: Long
    open fun createGlitch(cell: Cell): Glitch =
        Glitch(cell, glitchHp, glitchClonePeriod, controller.fighting.newTeamId())

    abstract val sharpySpawnCount: Int
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

    abstract val colorModificationSpawnCount: Int
    abstract val colorModificationRGBDeltaGenerationTable: GenerationTable<RGBDelta>
    open fun createColorModification(cell: Cell): ColorModificationEntity = ColorModificationEntity(
        cell, colorModificationRGBDeltaGenerationTable.roll()
    )

    abstract val instantHealSpawnCount: Int
    abstract val instantHealGenerationTable: GenerationTable<Int>
    open fun createInstantHeal(cell: Cell): InstantHealEntity = InstantHealEntity(
        cell, instantHealGenerationTable.roll()
    )
}

@Suppress("MagicNumber")
fun GenerationTableBuilder<RGBDelta>.addDefaults(eachWeight: Int = 1) = apply {
    outcome(eachWeight) { RGBDelta(20, 0, 0) }
    outcome(eachWeight) { RGBDelta(0, 20, 0) }
    outcome(eachWeight) { RGBDelta(0, 0, 20) }
    outcome(eachWeight) { RGBDelta(-10, -10, -10) }
}
