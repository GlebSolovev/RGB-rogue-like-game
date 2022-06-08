package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuilder
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.*
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta.DirectAttackHeroBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta.UponSeeingBehaviour
import ru.hse.sd.rgb.gamelogic.engines.fight.AttackType
import ru.hse.sd.rgb.gamelogic.engines.fight.ControlParams
import ru.hse.sd.rgb.gamelogic.engines.fight.HealType
import ru.hse.sd.rgb.gamelogic.engines.items.scriptitems.InstantHealEntity
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

        const val MAX_HP_TO_INSTANT_HEAL_AMOUNT_COEFFICIENT = 0.2
        const val ON_DIE_ITEM_DROP_PROBABILITY = 0.1

        const val ON_DIE_EXPERIENCE_POINTS = 10
    }

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit) = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.STAR_8, outlineColor)
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
            add {
                DropItemOnDie(entity, childBlock, ON_DIE_ITEM_DROP_PROBABILITY) {
                    InstantHealEntity(
                        units.first().cell,
                        (colorCellHp.maxHp * MAX_HP_TO_INSTANT_HEAL_AMOUNT_COEFFICIENT).toInt()
                    )
                }
            }
        }
        .add {
            UponSeeingBehaviour(entity, childBehaviour, controller.hero, seeingDepth) {
                DirectAttackHeroBehaviour(it, (movePeriodMillis * DIRECT_ATTACK_MOVE_PERIOD_COEFFICIENT).toLong())
            }
        }
        .build()
    override val behaviourEntity = BehaviourEntity()

    override val experienceEntity = object : ExperienceEntity() {
        override val onDieExperiencePoints: Int = ON_DIE_EXPERIENCE_POINTS
    }
}
