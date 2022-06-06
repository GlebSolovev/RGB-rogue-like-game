package ru.hse.sd.rgb.gamelogic.items

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuilder
import ru.hse.sd.rgb.gamelogic.engines.behaviour.NoneBehaviour
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.utils.Direction
import ru.hse.sd.rgb.utils.structures.RGB

abstract class BasicItemEntity(cell: Cell, color: RGB) : GameEntity(setOf(ColorCellNoHp(color, cell))) {

    companion object {
        const val DROP_TIMEOUT_MILLIS = 3000L
    }

    abstract override val viewEntity: ViewEntity

    protected abstract fun getNewItemUnconditionally(picker: GameEntity): Item

    // null if not ready to be picked up
    fun getNewItem(picker: GameEntity): Item? {
        if (lifeStartTimeMillis == null || System.currentTimeMillis() - lifeStartTimeMillis!! < DROP_TIMEOUT_MILLIS)
            return null
        return getNewItemUnconditionally(picker)
    }

    final override val physicalEntity = object : PhysicalEntity() {
        override val isSolid = false
        override fun getUnitDirection(unit: GameUnit, dir: Direction) = dir
    }

    final override val fightEntity = object : FightEntity() {
        override val teamId = controller.fighting.newTeamId()
        override fun isUnitActive(unit: GameUnit) = false
    }

    private val itemBaseBehaviour = NoneBehaviour(this)
    final override val lifecycle = BehaviourBuilder.lifecycle(this, itemBaseBehaviour).build()

    final override val behaviourEntity = SingleBehaviourEntity(itemBaseBehaviour)

    private var lifeStartTimeMillis: Long? = null

    override fun onLifeStart() {
        lifeStartTimeMillis = System.currentTimeMillis()
    }
}
