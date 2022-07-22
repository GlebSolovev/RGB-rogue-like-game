package ru.hse.sd.rgb.gamelogic.engines.experience.scriptactions

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.experience.ExperienceLevelAction
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.HpGameUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("increase_max_hp")
class IncreaseMaxHpAction(private val onPercent: Double) : ExperienceLevelAction {

    init {
        if (onPercent <= 0) throw IllegalArgumentException("onPercent must be positive")
    }

    @Suppress("MagicNumber") // 100.0 for %
    override suspend fun activate(onEntity: GameEntity) {
        for (unit in onEntity.units) {
            if (unit is HpGameUnit) {
                controller.fighting.changeMaxHp(unit, (unit.maxHp * (onPercent / 100.0)).toInt())
                controller.fighting.attackDirectly(unit, -unit.maxHp)
            }
        }
    }
}
