package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuilder
import ru.hse.sd.rgb.gamelogic.engines.behaviour.NoneBehaviour
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.utils.Direction
import ru.hse.sd.rgb.utils.ignore
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.views.ViewUnit
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

    private val wallBaseBehaviour = NoneBehaviour(this)
    override val lifecycle = BehaviourBuilder.lifecycle(this, wallBaseBehaviour).build()
    override val behaviourEntity = SingleBehaviourEntity(wallBaseBehaviour)
}
