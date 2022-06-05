package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuildingBlock
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.utils.Direction
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.EntityUpdated
import ru.hse.sd.rgb.utils.messaging.messages.MoveTick
import ru.hse.sd.rgb.utils.randomCell
import ru.hse.sd.rgb.utils.structures.Paths2D

class MoveUsingUpdatingPath(
    entity: GameEntity,
    childBlock: BehaviourBuildingBlock?,
    periodMillis: Long,
    private val calculatePathStrategy: () -> Paths2D.PathStrategy
) : BehaviourBuildingBlock(entity, childBlock) {

    override val ticker = Ticker(periodMillis, entity, MoveTick())

    override suspend fun handleMessage(message: Message) {
        if (message is MoveTick) {
            val pathStrategy = calculatePathStrategy()
            val cell = entity.units.first().cell
            val moved = controller.physics.tryMove(entity, pathStrategy.next(cell))
            if (moved) controller.view.receive(EntityUpdated(entity))
        } else {
            childBlock?.handleMessage(message)
        }
    }
}

fun MoveDirectlyTowards(
    entity: GameEntity, childBlock: BehaviourBuildingBlock?, periodMillis: Long, towards: GameEntity
) = MoveUsingUpdatingPath(entity, childBlock, periodMillis) {
    Paths2D.straightLine(
        entity.units.first().cell,
        towards.randomCell()
    )
}

fun MoveDirectlyFrom(
    entity: GameEntity, childBlock: BehaviourBuildingBlock?, periodMillis: Long, from: GameEntity
) = MoveUsingUpdatingPath(entity, childBlock, periodMillis) {
    Paths2D.straightLine(
        from.randomCell(),
        entity.units.first().cell
    )
}

fun MoveTowardsDirection(
    entity: GameEntity, childBlock: BehaviourBuildingBlock?, periodMillis: Long, direction: Direction
) = MoveUsingUpdatingPath(entity, childBlock, periodMillis) {
    object : Paths2D.PathStrategy {
        override fun next(from: Cell) = direction
    }
}

fun MoveUsingStaticPath(
    entity: GameEntity,
    childBlock: BehaviourBuildingBlock?,
    periodMillis: Long,
    pathStrategy: Paths2D.PathStrategy
) = MoveUsingUpdatingPath(entity, childBlock, periodMillis) { pathStrategy }

fun MoveDirectlyTowardsCell(
    entity: GameEntity, childBlock: BehaviourBuildingBlock?, periodMillis: Long, towards: Cell
) = MoveUsingStaticPath(
    entity, childBlock, periodMillis, Paths2D.straightLine(
        entity.units.first().cell,
        towards
    )
)
