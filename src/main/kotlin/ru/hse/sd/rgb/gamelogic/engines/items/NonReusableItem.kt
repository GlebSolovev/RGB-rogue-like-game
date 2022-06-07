package ru.hse.sd.rgb.gamelogic.engines.items

import ru.hse.sd.rgb.gamelogic.entities.GameEntity

abstract class NonReusableItem(holder: GameEntity) : Item(holder) {

    final override val isReusable: Boolean = false

    abstract inner class ViewNonReusableItem : ViewItem() {
        final override val isEquipped: Boolean = false
    }

}
