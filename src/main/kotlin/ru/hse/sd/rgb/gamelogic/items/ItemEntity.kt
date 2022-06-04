package ru.hse.sd.rgb.gamelogic.items

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuilder
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.utils.Direction
import ru.hse.sd.rgb.utils.structures.RGB

abstract class ItemEntity(cell: Cell, color: RGB) : GameEntity(setOf(ColorCellNoHp(color, cell))) {

    abstract override val viewEntity: ViewEntity

    abstract fun getNewItem(picker: GameEntity): Item

    final override val physicalEntity = object : PhysicalEntity() {
        override val isSolid = false
        override fun getUnitDirection(unit: GameUnit, dir: Direction) = dir
    }

    final override val fightEntity = object : FightEntity() {
        override val teamId = controller.fighting.newTeamId()
        override fun isUnitActive(unit: GameUnit) = false
    }

    final override val behaviour = BehaviourBuilder.lifecycle(this).build()

    final override val behaviourEntity = SingleBehaviourEntity(behaviour)
}
