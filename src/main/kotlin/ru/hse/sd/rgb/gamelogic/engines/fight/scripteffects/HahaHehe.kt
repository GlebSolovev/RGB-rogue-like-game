package ru.hse.sd.rgb.gamelogic.engines.fight.scripteffects

import kotlinx.serialization.Serializable

@Serializable
data class HahaHehe(
    val count: Int,
    val movePeriodMillis: Long,
    val fireballHp: Int,
    val isControllable: Boolean
)
