package ru.hse.sd.rgb.gamelogic.engines.fight.scripteffects

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.fight.*
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.IciclesBomb
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("icicles_bomb")
class IciclesBombEffect(
    private val count: Int,
    private val movePeriodMillis: Long,
    private val isControllable: Boolean,
    private val iciclesCount: Int,
    private val slowDownCoefficient: Double,
    private val frozenDurationMillis: Long?
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

            val iciclesBomb = IciclesBomb(
                ColorCellNoHp(unit.gameColor, unit.cell),
                movePeriodMillis,
                targetCell,
                iciclesCount,
                slowDownCoefficient,
                frozenDurationMillis,
                unit.parent.fightEntity.teamId
            )
            controller.creation.tryAddToWorld(iciclesBomb)
        }
    }
}
