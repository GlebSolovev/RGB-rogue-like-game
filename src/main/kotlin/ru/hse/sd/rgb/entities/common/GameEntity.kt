package ru.hse.sd.rgb.entities.common

import ru.hse.sd.rgb.Messagable
import ru.hse.sd.rgb.Message
import ru.hse.sd.rgb.views.GameEntityViewSnapshot
import ru.hse.sd.rgb.views.ViewUnit

abstract class GameEntity : Messagable() {

    abstract inner class ViewEntity {
        protected abstract fun convertUnit(unit: GameUnit): ViewUnit

        fun takeViewSnapshot(): GameEntityViewSnapshot {
            return units.map { convertUnit(it) }.toSet()
        }

        open fun applyMessageToAppearance(m: Message) {}
    }

    abstract inner class PhysicalEntity {
        abstract val isSolid: Boolean
    }

    abstract val physicalEntity: PhysicalEntity
    abstract val viewEntity: ViewEntity

    abstract val units: Set<GameUnit>
}
