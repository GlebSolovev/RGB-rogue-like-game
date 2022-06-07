package ru.hse.sd.rgb.gamelogic.engines.fight.scripteffects

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.fight.*
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Fireball
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("fireball")
class FireballEffect(
    private val count: Int,
    private val movePeriodMillis: Long,
    private val isControllable: Boolean,
    private val burningAttackPeriodMillis: Long,
    private val burningAttack: Int,
    private val burningDurationMillis: Long?,
) : BaseColorUpdateEffect {

    override suspend fun activate(
        unit: GameUnit,
        controlParams: ControlParams,
        unsafeMethods: FightEngine.UnsafeMethods
    ) {
        val attackType = if (!isControllable) AttackType.RANDOM_TARGET else controlParams.attackType
        if (attackType == AttackType.NO_ATTACK) return

        repeat(count) {
            val targetCell = attackTargetCell(attackType, unit, it)!!

            val fireball = Fireball(
                ColorCellNoHp(unit.gameColor, unit.cell),
                movePeriodMillis,
                targetCell,
                burningAttackPeriodMillis,
                burningAttack,
                burningDurationMillis,
                unit.parent.fightEntity.teamId
            )
            controller.creation.tryAddToWorld(fireball)
        }
    }
}
