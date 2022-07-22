package ru.hse.sd.rgb.gamelogic.engines.fight.scripteffects

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.fight.AttackType
import ru.hse.sd.rgb.gamelogic.engines.fight.BaseColorUpdateEffect
import ru.hse.sd.rgb.gamelogic.engines.fight.ControlParams
import ru.hse.sd.rgb.gamelogic.engines.fight.FightEngine
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.LaserPart
import ru.hse.sd.rgb.utils.structures.Direction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("laser")
class LaserEffect(
    private val persistMillis: Long,
) : BaseColorUpdateEffect {
    override suspend fun activate(
        unit: GameUnit,
        controlParams: ControlParams,
        unsafeMethods: FightEngine.UnsafeMethods
    ) {
        if (controlParams.attackType == AttackType.NO_ATTACK) return

        val dir = unit.lastMoveDir.takeUnless { dir: Direction -> dir == Direction.NOPE } ?: Direction.random()
        controller.creation.tryAddToWorld(
            LaserPart(
                ColorCellNoHp(unit.gameColor, unit.cell),
                persistMillis,
                dir,
                unit.parentTeamId
            )
        )
    }
}
