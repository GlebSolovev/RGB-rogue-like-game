package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.SimpleBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.State
import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.entities.*
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.messages.CollidedWith
import ru.hse.sd.rgb.utils.messaging.messages.ColorTick
import ru.hse.sd.rgb.utils.messaging.messages.ReceivedAttack
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.views.*
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class Wall(colorCells: Set<ColorCellNoHp>) : GameEntity(colorCells) {

    constructor(color: RGB, cell: Cell) : this(setOf(ColorCellNoHp(color, cell)))

    override val viewEntity: ViewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit): ViewUnit = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.SQUARE)
        }

        override fun applyMessageToAppearance(m: Message) = ignore
    }

    override val physicalEntity: PhysicalEntity = object : PhysicalEntity() {
        override val isSolid: Boolean = true
        override fun getUnitDirection(unit: GameUnit, dir: Direction): Direction = Direction.NOPE
    }

    override val fightEntity = object : FightEntity() {
        override fun isUnitActive(unit: GameUnit): Boolean = false
        override val teamId = controller.fighting.newTeamId()
    }

    override var behaviour: Behaviour = WallDefaultBehaviour()
    override val behaviourEntity = SingleBehaviourEntity(behaviour)

    private inner class WallDefaultBehaviour : SimpleBehaviour(this) {
        override var state = object : State() {
            override suspend fun handleReceivedAttack(message: ReceivedAttack): State = this
            override suspend fun handleCollidedWith(message: CollidedWith): State = this
            override suspend fun handleColorTick(tick: ColorTick): State = this
            override suspend fun handleMoveTick(): State = this
        }

        override fun stopTickers() = ignore
    }
}
