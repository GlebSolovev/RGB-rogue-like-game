package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta.AttackUponSeeingMetaBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.simple.PassiveBehaviour
import ru.hse.sd.rgb.gamelogic.entities.ColorCellHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.Direction
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class Sharpy(
    colorCellHp: ColorCellHp,
    private val movePeriodMillis: Long,
    private val seeingDepth: Int,
    private val watchPeriodMillis: Long,
    teamId: Int
) : GameEntity(setOf(colorCellHp)) {

    companion object {
        const val DIRECT_ATTACK_MOVE_PERIOD_COEFFICIENT: Double = 4.0
    }

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit) = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.STAR_8)
        }
    }

    override val physicalEntity = object : PhysicalEntity() {
        override val isSolid: Boolean = false
        override fun getUnitDirection(unit: GameUnit, dir: Direction): Direction = dir
    }

    override val fightEntity = object : FightEntity() {
        override fun isUnitActive(unit: GameUnit): Boolean = false
        override val teamId: Int = teamId
    }

    override fun onLifeStart() {
        behaviour = AttackUponSeeingMetaBehaviour(
            PassiveBehaviour(this, movePeriodMillis), this,
            controller.hero,
            seeingDepth,
            (movePeriodMillis / DIRECT_ATTACK_MOVE_PERIOD_COEFFICIENT).toLong(),
            watchPeriodMillis
        )
    }

    override lateinit var behaviour: Behaviour
    override val behaviourEntity = BehaviourEntity()
}
