package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuilder
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.AttackOnCollision
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.DieOnFatalAttack
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.EnableColorUpdate
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.MoveUsingUpdatingPath
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta.DirectAttackHeroBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta.UponSeeingBehaviour
import ru.hse.sd.rgb.gamelogic.engines.fight.AttackType
import ru.hse.sd.rgb.gamelogic.engines.fight.ControlParams
import ru.hse.sd.rgb.gamelogic.engines.fight.HealType
import ru.hse.sd.rgb.gamelogic.entities.ColorCellHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.structures.Direction
import ru.hse.sd.rgb.utils.structures.Paths2D
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class Sharpy(
    colorCellHp: ColorCellHp,
    private val movePeriodMillis: Long,
    private val seeingDepth: Int,
    teamId: Int
) : GameEntity(setOf(colorCellHp)) {

    companion object {
        const val DIRECT_ATTACK_MOVE_PERIOD_COEFFICIENT: Double = 0.25
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

    override val lifecycle = BehaviourBuilder.lifecycle(this)
        .addBlocks {
            add { MoveUsingUpdatingPath(entity, childBlock, movePeriodMillis) { Paths2D.randomWalk() } }
            add {
                EnableColorUpdate(
                    entity,
                    childBlock,
                    ControlParams(AttackType.RANDOM_TARGET, HealType.RANDOM_TARGET)
                )
            }
            add { AttackOnCollision(entity, childBlock) }
            add { DieOnFatalAttack(entity, childBlock) }
        }
        .add {
            UponSeeingBehaviour(entity, childBehaviour, controller.hero, seeingDepth) {
                DirectAttackHeroBehaviour(it, (movePeriodMillis * DIRECT_ATTACK_MOVE_PERIOD_COEFFICIENT).toLong())
            }
        }
        .build()
    override val behaviourEntity = BehaviourEntity()
}
