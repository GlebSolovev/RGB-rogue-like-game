package ru.hse.sd.rgb.gamelogic.engines.items

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuilder
import ru.hse.sd.rgb.gamelogic.engines.behaviour.NoneBehaviour
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.utils.structures.Direction
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance

sealed class Item(protected val holder: GameEntity) {

    companion object {
        const val DEFAULT_ITEM_INVENTORY_VIEW_UNIT_SCALE = 0.9
        const val DROP_TIMEOUT_MILLIS = 3000L
    }

    abstract val isReusable: Boolean

    abstract inner class ViewItem {
        abstract fun getSwingAppearance(): SwingUnitAppearance

        abstract val color: RGB
        abstract val description: String
        abstract val isEquipped: Boolean
    }

    abstract val viewItem: ViewItem

    // TODO: make ItemEntity abstract inner class and create it using static method => don't forget about respawned
    abstract fun getNewItemEntity(cell: Cell): ItemEntity

    abstract suspend fun use()

    abstract fun extractPersistence(): ItemPersistence
}

abstract class ItemPersistence {
    abstract fun convertToItem(holder: GameEntity): Item
}

abstract class ItemEntity(cell: Cell, color: RGB, private val respawned: Boolean) :
    GameEntity(setOf(ColorCellNoHp(color, cell))) {

    private var lifeStartTimeMillis: Long? = null

    override suspend fun onLifeStart() {
        lifeStartTimeMillis = System.currentTimeMillis()
    }

    protected abstract fun getNewItemUnconditionally(picker: GameEntity): Item

    // null if not ready to be picked up
    // MUST BE THREAD-SAFE!
    fun getNewItem(picker: GameEntity): Item? {
        if (respawned && cannotBePickedBecauseOfTimeout) {
            return null
        }
        return getNewItemUnconditionally(picker)
    }

    private val cannotBePickedBecauseOfTimeout: Boolean
        get() = run {
            val spawnedJustNow = lifeStartTimeMillis == null
            spawnedJustNow || System.currentTimeMillis() - lifeStartTimeMillis!! < Item.DROP_TIMEOUT_MILLIS
        }
}

abstract class BasicItemEntity(cell: Cell, color: RGB, respawned: Boolean) : ItemEntity(cell, color, respawned) {

    final override val physicalEntity = object : PhysicalEntity() {
        override val isSolid = false
        override fun getUnitDirection(unit: GameUnit, dir: Direction) = dir
    }

    final override val fightEntity = object : FightEntity() {
        override val teamId = controller.fighting.newTeamId()
        override fun isUnitActive(unit: GameUnit) = false
    }

    private val itemBaseBehaviour = NoneBehaviour(this)
    final override val behaviourEntity = SingleBehaviourEntity(itemBaseBehaviour)

    final override val experienceEntity = object : ExperienceEntity() {
        override val onDieExperiencePoints: Int? = null
    }

    final override val lifecycle = BehaviourBuilder.lifecycle(this, itemBaseBehaviour).build()
}

interface ItemEntityCreator {
    fun createAt(cell: Cell): ItemEntity
}
