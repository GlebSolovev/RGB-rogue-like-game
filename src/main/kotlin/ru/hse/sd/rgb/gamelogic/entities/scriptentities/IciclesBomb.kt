package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuilder
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuildingBlock
import ru.hse.sd.rgb.gamelogic.engines.behaviour.NoneBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.*
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.messages.Dying
import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.utils.structures.Direction
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

@Suppress("LongParameterList")
class IciclesBomb(
    colorCell: ColorCellNoHp,
    movePeriodMillis: Long,
    targetCell: Cell,
    iciclesCount: Int,
    private val slowDownCoefficient: Double,
    private val frozenDurationMillis: Long?,
    teamId: Int,
) : GameEntity(setOf(colorCell)) {

    companion object {
        const val ICICLES_MOVE_PERIOD_MILLIS_COEFFICIENT = 0.25
    }

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit): ViewUnit = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.STAR_4, outlineColor)
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

    private val icicleBallBaseBehaviour = BehaviourBuilder.metaFromBlocks(NoneBehaviour(this))
        .add { MoveDirectlyTowardsCell(entity, childBlock, movePeriodMillis, targetCell) }
        .add { FreezeEnemyOnCollision(entity, childBlock, slowDownCoefficient, frozenDurationMillis) }
        .add { AttackOnCollision(entity, childBlock) }
        .add {
            object : BehaviourBuildingBlock(entity, childBlock) {
                override suspend fun handleMessage(message: Message) {
                    if (message is Dying) {
                        repeat(iciclesCount) {
                            val ballUnit = units.first()
                            val icicleTargetCell = controller.physics.generateRandomTarget(this@IciclesBomb)
                            val icicle = Icicle(
                                ColorCellNoHp(ballUnit.gameColor, ballUnit.cell),
                                (movePeriodMillis * ICICLES_MOVE_PERIOD_MILLIS_COEFFICIENT).toLong(),
                                icicleTargetCell,
                                slowDownCoefficient,
                                frozenDurationMillis,
                                fightEntity.teamId
                            )
                            controller.creation.tryAddToWorld(icicle)
                        }
                    }
                    childBlock?.handleMessage(message)
                }
            }
        }
        .add { DieOnCollisionWithOtherTeam(entity, childBlock) }
        .add { IgnoreCollisionsWithItems(entity, childBlock) }
        .add { DieOnFatalAttack(entity, childBlock) }
        .build()
    override val behaviourEntity = SingleBehaviourEntity(icicleBallBaseBehaviour)

    override val lifecycle = BehaviourBuilder.lifecycle(this)
        .add { icicleBallBaseBehaviour }
        .build()
}
