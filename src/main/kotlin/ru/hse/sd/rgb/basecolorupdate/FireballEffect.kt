package ru.hse.sd.rgb.basecolorupdate

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.entities.Fireball
import ru.hse.sd.rgb.entities.common.ColorCell
import ru.hse.sd.rgb.entities.common.GameUnit
import ru.hse.sd.rgb.logic.FightLogic
import ru.hse.sd.rgb.randomCell
import ru.hse.sd.rgb.unreachable

class FireballEffect(
    private val count: Int,
    private val movePeriodMillis: Long,
    private val isControllable: Boolean
) : BaseColorUpdateEffect {
    override suspend fun activate(
        unit: GameUnit,
        controlParams: ControlParams,
        unsafeMethods: FightLogic.UnsafeMethods
    ) {
        val attackType = if (!isControllable) AttackType.RANDOM_TARGET else controlParams.attackType
        if (attackType == AttackType.NO_ATTACK) return

        repeat(count) {
            val targetCell = when (attackType) {
                AttackType.HERO_TARGET -> controller.hero.randomCell()
                AttackType.RANDOM_TARGET -> controller.physics.generateRandomTarget(unit.parent)
                else -> unreachable
            }
            val fireball = Fireball(ColorCell(unit.gameColor, unit.cell), movePeriodMillis, targetCell)
            controller.creation.tryAddToWorld(fireball)
        }
    }
}