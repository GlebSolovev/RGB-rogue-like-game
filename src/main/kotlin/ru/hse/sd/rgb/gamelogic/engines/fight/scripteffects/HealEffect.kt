package ru.hse.sd.rgb.gamelogic.engines.fight.scripteffects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.hse.sd.rgb.gamelogic.engines.fight.BaseColorUpdateEffect
import ru.hse.sd.rgb.gamelogic.engines.fight.ControlParams
import ru.hse.sd.rgb.gamelogic.engines.fight.FightEngine
import ru.hse.sd.rgb.gamelogic.engines.fight.HealType
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.gamelogic.entities.HpGameUnit

@Serializable
@SerialName("heal")
class HealEffect(
    private val isControllable: Boolean,
) : BaseColorUpdateEffect {

    override suspend fun activate(
        unit: GameUnit,
        controlParams: ControlParams,
        unsafeMethods: FightEngine.UnsafeMethods
    ) {
        val healType = if (isControllable) controlParams.healType else HealType.RANDOM_TARGET
        val targetUnit: GameUnit = when (healType) {
            HealType.NO_HEAL -> null
            HealType.LOWEST_HP_TARGET -> {
                unit.parent.units
                    .asSequence()
                    .filterIsInstance<HpGameUnit>()
                    .minByOrNull { it.hp.toDouble() / it.maxHp }
            }
            HealType.RANDOM_TARGET -> {
                unit.parent.units
                    .asSequence()
                    .filterIsInstance<HpGameUnit>()
                    .shuffled()
                    .firstOrNull()
            }
        } ?: return

        unsafeMethods.unsafeAttack(unit, targetUnit) // TODO: this is a cursed api
    }

}