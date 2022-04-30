package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gameloaders.InventoryDescription
import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.engines.items.Inventory
import ru.hse.sd.rgb.gamelogic.entities.*
import ru.hse.sd.rgb.utils.Direction
import ru.hse.sd.rgb.utils.Message
import ru.hse.sd.rgb.utils.unreachable
import ru.hse.sd.rgb.views.*
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class Hero(
    colorHpCells: Set<ColorHpCell>,
    invDesc: InventoryDescription
) : GameEntity(colorHpCells) {

    inner class PhysicalHero : PhysicalEntity() {
        override val isSolid = true
        override fun getUnitDirection(unit: GameUnit, dir: Direction): Direction = dir
    }

    inner class ViewHero : ViewEntity() {
        override fun convertUnit(unit: GameUnit): ViewUnit = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.SQUARE)
        }
    }

    override val physicalEntity = PhysicalHero()
    override val viewEntity = ViewHero()

    override fun onGameStart() {
        controller.view.receive(View.SubscribeToMovement(this))
    }

    private val inventory: Inventory = Inventory(invDesc.invGridW, invDesc.invGridH)

    // automaton start

    private abstract inner class HeroState {
        abstract suspend fun next(m: Message): HeroState
    }

    private inner class PlayingState : HeroState() {
        override suspend fun next(m: Message) = when (m) {
            is UserMoved -> this.also {
                val moved = controller.physics.tryMove(this@Hero, m.dir)
                if (moved) controller.view.receive(EntityMoved(this@Hero))
            }
            is CollidedWith -> this.also {
                controller.fighting.attack(m.myUnit, m.otherUnit)
                // TODO: pick item instead of attacking
            }
            is ColorTick -> this.also {
//                addArgs = Struct { attackTarget: NEAREST, healTarget: LOW_HP }
//                state.get(unit) -> fightEntity.get(unit)
//                controller.fighting.update(m.unit, addArgs)
                // TODO: unit update in fighting logic
            }
            is UserToggledInventory -> InventoryState().also {
                controller.view.receive(InventoryOpened(inventory))
            }
            else -> unreachable
        }
    }

    private inner class InventoryState : HeroState() {
        private fun sendInvUpdate() = controller.view.receive(InventoryUpdated(inventory))
        override suspend fun next(m: Message) = when (m) {
            is UserMoved -> this.also {
                inventory.moveSelection(m.dir)
                sendInvUpdate()
            }
            is UserToggledInventory -> PlayingState().also {
                controller.view.receive(InventoryClosed())
            }
            is UserSelect -> this.also {
                inventory.useCurrent()
                sendInvUpdate()
            }
            is UserDrop -> this.also {
                inventory.dropCurrent()
                // TODO: place item in world
                sendInvUpdate()
            }
            else -> unreachable
        }
    }

    private var state: HeroState = PlayingState()

    // automaton end

    override suspend fun handleGameMessage(m: Message) {
        state = state.next(m)
    }

}
