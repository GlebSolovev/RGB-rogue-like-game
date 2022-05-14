package ru.hse.sd.rgb.gamelogic.engines.fight.scripteffects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.hse.sd.rgb.gamelogic.engines.fight.AttackType
import ru.hse.sd.rgb.gamelogic.engines.fight.BaseColorUpdateEffect
import ru.hse.sd.rgb.gamelogic.engines.fight.ControlParams
import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.Fireball
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.gamelogic.engines.fight.FightEngine
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.utils.*

@Serializable
@SerialName("fireball")
class FireballEffect(
    private val count: Int,
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

        repeat(count) {
            val targetCell = when (attackType) {
                AttackType.HERO_TARGET -> controller.hero.randomCell()
                AttackType.RANDOM_TARGET -> controller.physics.generateRandomTarget(unit.parent)
                AttackType.LAST_MOVE_DIR ->
                    if (it == 0) unit.cell + unit.lastMoveDir.toShift()
                    else controller.physics.generateRandomTarget(unit.parent)
                else -> unreachable
            }

            val fireball = Fireball(
                ColorCellNoHp(unit.gameColor, unit.cell),
                movePeriodMillis,
                targetCell,
                unit.parent.fightEntity.teamId
            )
            controller.creation.tryAddToWorld(fireball)
        }
    }
}