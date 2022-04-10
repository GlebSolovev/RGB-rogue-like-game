package ru.hse.sd.rgb.entities.common

import ru.hse.sd.rgb.Messagable

abstract class GameEntity : Messagable() {
    abstract val units: Set<GameUnit>

    abstract val physicalEntity: PhysicalEntity
    abstract val viewEntity: ViewEntity
}
