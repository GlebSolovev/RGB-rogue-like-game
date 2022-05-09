package ru.hse.sd.rgb.gamelogic.engines.fight.scripteffects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.engines.fight.AttackType
import ru.hse.sd.rgb.gamelogic.engines.fight.BaseColorUpdateEffect
import ru.hse.sd.rgb.gamelogic.engines.fight.ControlParams
import ru.hse.sd.rgb.gamelogic.engines.fight.FightEngine
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.WavePart
import ru.hse.sd.rgb.utils.Direction
import ru.hse.sd.rgb.utils.GridShift
import ru.hse.sd.rgb.utils.plus

@Serializable
@SerialName("wave")
class WaveEffect(
    private val width: Int,
    private val movePeriodMillis: Long,
) : BaseColorUpdateEffect {

    override suspend fun activate(
        unit: GameUnit,
        controlParams: ControlParams,
        unsafeMethods: FightEngine.UnsafeMethods
    ) {
        if (controlParams.attackType == AttackType.NO_ATTACK) return
        if (width % 2 != 1) throw IllegalArgumentException("width must be odd")

        val dir = Direction.random()
        val radius = width / 2
        val center = unit.cell + dir.toShift()

        fun calcTarget(t: Int) = center + GridShift(if (dir.isVertical) t else 0, if (dir.isHorizontal) t else 0)
        suspend fun spawnWavePart(t: Int) = controller.creation.tryAddToWorld(
            WavePart(ColorCellNoHp(unit.gameColor, calcTarget(t)), movePeriodMillis, dir)
        )

        spawnWavePart(0)
        for (t in 1..radius) if (!spawnWavePart(t)) break
        for (t in -1 downTo-radius) if (!spawnWavePart(t)) break
    }

}