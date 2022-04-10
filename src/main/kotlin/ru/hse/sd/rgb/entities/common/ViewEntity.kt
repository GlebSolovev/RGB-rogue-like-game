package ru.hse.sd.rgb.entities.common

import ru.hse.sd.rgb.Message
import ru.hse.sd.rgb.views.GameEntityViewSnapshot

abstract class ViewEntity {
    abstract fun convert(units: Set<GameUnit>): GameEntityViewSnapshot
    abstract fun applyMessageToAppearance(m: Message)
}
