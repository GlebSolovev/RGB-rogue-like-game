package ru.hse.sd.rgb.basecolorupdate

import ru.hse.sd.rgb.entities.common.GameUnit
import ru.hse.sd.rgb.logic.FightLogic

// Entity, where do you want to fight?
enum class AttackType { NO_ATTACK, HERO_TARGET, RANDOM_TARGET }
enum class HealType { NO_HEAL, LOWEST_HP_TARGET, RANDOM_TARGET }

data class ControlParams(val attackType: AttackType, val healType: HealType)

interface BaseColorUpdateEffect {

    suspend fun activate(
        unit: GameUnit,
        controlParams: ControlParams,
        unsafeMethods: FightLogic.UnsafeMethods
    ) // use only unsafeMethods to interact with FightLogic

}