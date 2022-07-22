package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuilder
import ru.hse.sd.rgb.gamelogic.engines.behaviour.NoneBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.*
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.structures.Direction
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class WavePart(
    cell: ColorCellNoHp,
    movePeriodMillis: Long,
    dir: Direction,
    teamId: Int,
) : GameEntity(setOf(cell)) {

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit) = object : ViewUnit(unit) {
            override val swingAppearance: SwingUnitAppearance
                get() = SwingUnitAppearance(SwingUnitShape.CIRCLE_HALF(dir), outlineColor)
        }
    }

    override val physicalEntity = object : PhysicalEntity() {
        override val isSolid = false
        override fun getUnitDirection(unit: GameUnit, dir: Direction) = dir
    }

    override val fightEntity = object : FightEntity() {
        override fun isUnitActive(unit: GameUnit) = false
        override val teamId = teamId
    }

    private val wavePartBaseBehaviour = NoneBehaviour(this)
    override val lifecycle = BehaviourBuilder.lifecycle(this, wavePartBaseBehaviour)
        .addBlocks {
            add { DieOnFatalAttack(entity, childBlock) }
            add { AttackOnCollision(entity, childBlock) }
            add { DieOnCollisionWithOtherTeam(entity, childBlock) }
            add { IgnoreCollisionsWithItems(entity, childBlock) }
            add { MoveTowardsDirection(entity, childBlock, movePeriodMillis, dir) }
        }.build()

    override val behaviourEntity = SingleBehaviourEntity(wavePartBaseBehaviour)

    override val experienceEntity = object : ExperienceEntity() {
        override val onDieExperiencePoints: Int? = null
    }
}
