package ru.hse.sd.rgb.gamelogic.engines.fight

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.plus
import ru.hse.sd.rgb.utils.randomCell

// Entity, where do you want to fight?
enum class AttackType { NO_ATTACK, HERO_TARGET, RANDOM_TARGET, LAST_MOVE_DIR }
enum class HealType { NO_HEAL, LOWEST_HP_TARGET, RANDOM_TARGET }

data class ControlParams(val attackType: AttackType, val healType: HealType)

interface BaseColorUpdateEffect {
    suspend fun activate(
        unit: GameUnit,
        controlParams: ControlParams,
        unsafeMethods: FightEngine.UnsafeMethods
    ) // use only unsafeMethods to interact with FightLogic
}

fun attackTargetCell(attackType: AttackType, unit: GameUnit, iterCount: Int = 0) = when (attackType) {
    AttackType.HERO_TARGET -> controller.hero.randomCell()
    AttackType.RANDOM_TARGET -> controller.physics.generateRandomTarget(unit.parent)
    AttackType.LAST_MOVE_DIR ->
        if (iterCount == 0) unit.cell + unit.lastMoveDir.toShift()
        else controller.physics.generateRandomTarget(unit.parent)
    AttackType.NO_ATTACK -> null
}
