package ru.hse.sd.rgb.gamelogic.engines.fight.scripteffects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.fight.AttackType
import ru.hse.sd.rgb.gamelogic.engines.fight.BaseColorUpdateEffect
import ru.hse.sd.rgb.gamelogic.engines.fight.ControlParams
import ru.hse.sd.rgb.gamelogic.engines.fight.FightEngine
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.WavePart
import ru.hse.sd.rgb.utils.*
import kotlin.math.abs

@Serializable
@SerialName("wave")
class WaveEffect(
    private val width: Int,
    private val movePeriodMillis: Long,
    private val isControllable: Boolean
) : BaseColorUpdateEffect {

    override suspend fun activate(
        unit: GameUnit,
        controlParams: ControlParams,
        unsafeMethods: FightEngine.UnsafeMethods
    ) {
        val attackType = if (!isControllable) AttackType.RANDOM_TARGET else controlParams.attackType
        if (attackType == AttackType.NO_ATTACK) return
        if (width % 2 != 1) throw IllegalArgumentException("width must be odd")

        fun calcDirTo(targetCell: Cell): Direction {
            val dx = unit.cell.x - targetCell.x
            val dy = unit.cell.y - targetCell.y
            return if (abs(dx) < abs(dy)) if(dx > 0) Direction.RIGHT else Direction.LEFT else if(dy > 0) Direction.UP else Direction.DOWN
        }
        val dir = when (attackType) {
            AttackType.HERO_TARGET -> calcDirTo(controller.hero.randomCell())
            AttackType.RANDOM_TARGET -> Direction.random()
            AttackType.LAST_MOVE_DIR -> unit.lastMoveDir
            else -> unreachable
        }
        val radius = width / 2
        val center = unit.cell + dir.toShift()

        fun calcTarget(t: Int) = center + GridShift(if (dir.isVertical) t else 0, if (dir.isHorizontal) t else 0)
        suspend fun spawnWavePart(t: Int) = controller.creation.tryAddToWorld(
            WavePart(
                ColorCellNoHp(unit.gameColor, calcTarget(t)),
                movePeriodMillis,
                dir,
                unit.parent.fightEntity.teamId
            )
        )

        spawnWavePart(0)
        for (t in 1..radius) if (!spawnWavePart(t)) break
        for (t in -1 downTo-radius) if (!spawnWavePart(t)) break
    }

}