package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuilder
import ru.hse.sd.rgb.gamelogic.engines.behaviour.NoneBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.*
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.utils.structures.Direction
import ru.hse.sd.rgb.utils.structures.Paths2D
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class Icicle(
    colorCell: ColorCellNoHp,
    movePeriodMillis: Long,
    targetCell: Cell,
    private val slowDownCoefficient: Double,
    private val frozenDurationMillis: Long?,
    teamId: Int,
) : GameEntity(setOf(colorCell)) {

    companion object {
        const val ICICLE_SWING_VIEW_APPEARANCE_SCALE = 0.7
    }

    private val path = Paths2D.straightLine(colorCell.cell, targetCell)

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit): ViewUnit = object : ViewUnit(unit) {
            override val swingAppearance: SwingUnitAppearance
                get() {
                    val currentDir = path.next(units.first().cell)
                    return SwingUnitAppearance(
                        SwingUnitShape.TRIANGLE(currentDir),
                        outlineColor,
                        ICICLE_SWING_VIEW_APPEARANCE_SCALE
                    )
                }
        }
    }

    override val physicalEntity = object : PhysicalEntity() {
        override val isSolid = false
        override fun getUnitDirection(unit: GameUnit, dir: Direction): Direction = dir
    }

    override val fightEntity = object : FightEntity() {
        override fun isUnitActive(unit: GameUnit): Boolean = false
        override val teamId = teamId
    }

    private val icicleBaseBehaviour = BehaviourBuilder.metaFromBlocks(NoneBehaviour(this))
        .add { MoveUsingStaticPath(entity, childBlock, movePeriodMillis, path) }
        .add { AttackOnCollision(entity, childBlock) }
        .add { FreezeEnemyOnCollision(entity, childBlock, slowDownCoefficient, frozenDurationMillis) }
        .add { DieOnCollisionWithOtherTeam(entity, childBlock) }
        .add { IgnoreCollisionsWithItems(entity, childBlock) }
        .add { DieOnFatalAttack(entity, childBlock) }
        .build()
    override val behaviourEntity = SingleBehaviourEntity(icicleBaseBehaviour)

    override val experienceEntity = object : ExperienceEntity() {
        override val onDieExperiencePoints: Int? = null
    }

    override val lifecycle = BehaviourBuilder.lifecycle(this)
        .add { icicleBaseBehaviour }
        .build()
}
