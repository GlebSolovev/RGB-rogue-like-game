package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuilder
import ru.hse.sd.rgb.gamelogic.engines.behaviour.NoneBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.ConfuseOnCollision
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.DieOnCollisionWithOtherTeam
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.DieOnFatalAttack
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.MoveDirectlyTowardsCell
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.utils.structures.Direction
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class ConfuseBall(
    colorCell: ColorCellNoHp,
    movePeriodMillis: Long,
    private val confuseDurationMillis: Long,
    targetCell: Cell,
    teamId: Int,
) : GameEntity(setOf(colorCell)) {

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit): ViewUnit = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.SPIRAL, outlineColor)
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

    private val confuseBallBaseBehaviour = BehaviourBuilder.metaFromBlocks(NoneBehaviour(this))
        .add { MoveDirectlyTowardsCell(entity, childBlock, movePeriodMillis, targetCell) }
        .add { ConfuseOnCollision(entity, childBlock, confuseDurationMillis) }
        .add { DieOnCollisionWithOtherTeam(entity, childBlock) }
        .add { DieOnFatalAttack(entity, childBlock) }
        .build()
    override val behaviourEntity = SingleBehaviourEntity(confuseBallBaseBehaviour)

    override val experienceEntity = object : ExperienceEntity() {
        override val onDieExperiencePoints: Int? = null
    }

    override val lifecycle = BehaviourBuilder.lifecycle(this)
        .add { confuseBallBaseBehaviour }
        .build()
}
