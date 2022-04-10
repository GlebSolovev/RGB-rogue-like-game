package ru.hse.sd.rgb.views

import ru.hse.sd.rgb.entities.common.GameUnit

// is a snapshot
abstract class ViewUnit(parentUnit: GameUnit) { // TODO: add layers?

    val cell = parentUnit.cell

    abstract fun getSwingAppearance(): SwingEntityAppearance

}

typealias GameEntityViewSnapshot = Set<ViewUnit>