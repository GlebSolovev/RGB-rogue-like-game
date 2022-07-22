package ru.hse.sd.rgb.gamelogic.engines.fight.scripteffects

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.fight.*
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.ConfuseBall
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
            unit.parentTeamId
        )
        controller.creation.tryAddToWorld(confuseBall)
    }
}
