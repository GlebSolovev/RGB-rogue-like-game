package ru.hse.sd.rgb.gamelogic.engines.fight.scripteffects

import ru.hse.sd.rgb.gamelogic.engines.fight.*
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.gamelogic.entities.HpGameUnit
import ru.hse.sd.rgb.utils.unreachable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
        if (healType == HealType.NO_HEAL) return

        val targetUnit: GameUnit = when (healType) {
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
            else -> unreachable
        } ?: return

        unsafeMethods.unsafeAttack(unit, targetUnit)
    }
}
