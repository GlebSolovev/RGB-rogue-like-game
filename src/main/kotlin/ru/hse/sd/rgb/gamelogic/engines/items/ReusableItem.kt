package ru.hse.sd.rgb.gamelogic.engines.items

import ru.hse.sd.rgb.gamelogic.entities.GameEntity

abstract class ReusableItem(holder: GameEntity, isEquipped: Boolean) : Item(holder) {

    final override val isReusable: Boolean = true

    private enum class EquippedState {
        EQUIPPED, UNEQUIPPED
    }

    private var equippedState = if (isEquipped) EquippedState.EQUIPPED else EquippedState.UNEQUIPPED

    abstract inner class ViewReusableItem : ViewItem() {
        final override val isEquipped: Boolean
            get() = isEquipped()

        // TODO: isEquipped in description
    }

    final override suspend fun use() {
        equippedState = when (equippedState) {
            EquippedState.EQUIPPED -> EquippedState.UNEQUIPPED.also { unequip() }
            EquippedState.UNEQUIPPED -> EquippedState.EQUIPPED.also { equip() }
        }
    }

    fun isEquipped(): Boolean = equippedState == EquippedState.EQUIPPED

    protected abstract suspend fun equip()

    protected abstract suspend fun unequip()

    abstract inner class ReusableItemPersistence : ItemPersistence() {
        protected val isEquipped = isEquipped()
    }
}
