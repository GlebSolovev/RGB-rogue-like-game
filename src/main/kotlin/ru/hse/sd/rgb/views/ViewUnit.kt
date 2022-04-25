package ru.hse.sd.rgb.views

import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance

// is a snapshot
// same class for all views
abstract class ViewUnit(parentUnit: GameUnit) { // TODO: add layers?

    val cell = parentUnit.cell
    val rgb = parentUnit.gameColor.rgb

    abstract val swingAppearance: SwingUnitAppearance

}

typealias GameEntityViewSnapshot = Set<ViewUnit>
