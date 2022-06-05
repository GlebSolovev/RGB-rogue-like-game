package ru.hse.sd.rgb.gamelogic.engines.fight.scripteffects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.engines.fight.*
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.ConfuseBall

@Serializable
@SerialName("confuse")
class ConfuseEffect(
    private val movePeriodMillis: Long,
    private val confuseDurationMillis: Long,
) : BaseColorUpdateEffect {

    override suspend fun activate(
        unit: GameUnit,
        controlParams: ControlParams,
        unsafeMethods: FightEngine.UnsafeMethods
    ) {
        val targetCell = attackTargetCell(controlParams.attackType, unit) ?: return

        val confuseBall = ConfuseBall(
            ColorCellNoHp(unit.gameColor, unit.cell),
            movePeriodMillis,
            confuseDurationMillis,
            targetCell,
            unit.parent.fightEntity.teamId
        )
        controller.creation.tryAddToWorld(confuseBall)
    }
}
