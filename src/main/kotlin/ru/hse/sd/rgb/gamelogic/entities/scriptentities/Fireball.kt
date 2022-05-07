package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.utils.Ticker.Companion.Ticker
import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.entities.*
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.views.EntityUpdated
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape
import kotlin.math.abs

class Fireball(
    colorCell: ColorCellNoHp,
    movePeriodMillis: Long,
    private val targetCell: Cell
) : GameEntity(setOf(colorCell)) {

    val ticker = Ticker(movePeriodMillis, MoveTick()).also { it.start() }
    // TODO: possible update ticker via effects

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit): ViewUnit = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.CIRCLE)
        }
    }

    override val physicalEntity = object : PhysicalEntity() {
        override val isSolid = false
        override fun getUnitDirection(unit: GameUnit, dir: Direction): Direction = dir
    }

    override val fightEntity = object : FightEntity() {
        override fun isUnitActive(unit: GameUnit): Boolean = true
    }

    private lateinit var lastDir: Direction
    private var targetReached = false


    override suspend fun handleGameMessage(m: Message) {
        when (m) {
            is MoveTick -> {
                val cell = units.first().cell

                if (cell == targetCell)
                    targetReached = true

                val dx = targetCell.x - cell.x
                val dy = targetCell.y - cell.y
                val dir = if (targetReached) lastDir else {
                    if (abs(dx) > abs(dy)) {
                        if (dx > 0) Direction.RIGHT else Direction.LEFT
                    } else {
                        if (dy > 0) Direction.DOWN else Direction.UP
                    }
                }
                lastDir = dir

                val moved = controller.physics.tryMove(this, dir)
                if (moved) controller.view.receive(EntityUpdated(this))
            }
            is CollidedWith -> {
                controller.fighting.attack(m.myUnit, m.otherUnit)
                controller.creation.die(this)
            }
            is ReceivedAttack -> if (m.isFatal) controller.creation.die(this)
            is ColorTick -> ignore
            else -> unreachable
        }
    }
}