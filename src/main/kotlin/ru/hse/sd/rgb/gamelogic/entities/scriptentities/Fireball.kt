package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuilder
import ru.hse.sd.rgb.gamelogic.engines.behaviour.NoneBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.*
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.utils.structures.Direction
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

@Suppress("LongParameterList")
class Fireball(
    colorCell: ColorCellNoHp,
    movePeriodMillis: Long,
    targetCell: Cell,
    burningAttackPeriodMillis: Long,
    burningAttack: Int,
    burningDurationMillis: Long?,
    teamId: Int,
) : GameEntity(setOf(colorCell)) {

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit): ViewUnit = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.CIRCLE, outlineColor)
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

    private val behaviour = BehaviourBuilder.metaFromBlocks(NoneBehaviour(this))
        .add { MoveDirectlyTowardsCell(entity, childBlock, movePeriodMillis, targetCell) }
        .add { AttackOnCollision(entity, childBlock) }
        .add { AnnihilateItems(entity, childBlock) }
        .add {
            FireEnemyOnCollision(entity, childBlock, burningAttackPeriodMillis, burningAttack, burningDurationMillis)
        }
        .add { DieOnCollisionWithOtherTeam(entity, childBlock) }
        .add { DieOnFatalAttack(entity, childBlock) }
        .build()
    override val behaviourEntity = SingleBehaviourEntity(behaviour)

    override val lifecycle = BehaviourBuilder.lifecycle(this)
        .add { behaviour }
        .build()
}
