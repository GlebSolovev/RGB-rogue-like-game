package ru.hse.sd.rgb.gamelogic.engines.fight

import ru.hse.sd.rgb.gamelogic.entities.GameUnit

// Entity, where do you want to fight?
enum class AttackType { NO_ATTACK, HERO_TARGET, RANDOM_TARGET }
enum class HealType { NO_HEAL, LOWEST_HP_TARGET, RANDOM_TARGET } // TODO: use in script effects

data class ControlParams(val attackType: AttackType, val healType: HealType)

interface BaseColorUpdateEffect {

    suspend fun activate(
        unit: GameUnit,
        controlParams: ControlParams,
        unsafeMethods: FightEngine.UnsafeMethods
    ) // use only unsafeMethods to interact with FightLogic

}