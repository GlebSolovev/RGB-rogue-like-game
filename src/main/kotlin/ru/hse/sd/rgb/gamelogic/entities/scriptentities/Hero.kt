package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gameloaders.InventoryDescription
import ru.hse.sd.rgb.gamelogic.FinishControllerMessage
import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.engines.fight.AttackType
import ru.hse.sd.rgb.gamelogic.engines.fight.ControlParams
import ru.hse.sd.rgb.gamelogic.engines.fight.HealType
import ru.hse.sd.rgb.gamelogic.engines.items.Inventory
import ru.hse.sd.rgb.gamelogic.entities.*
import ru.hse.sd.rgb.utils.Direction
import ru.hse.sd.rgb.utils.Message
import ru.hse.sd.rgb.utils.unreachable
import ru.hse.sd.rgb.views.*
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class Hero(
    colorCells: Set<ColorCellHp>,
    invDesc: InventoryDescription,
    private var singleDirMovePeriodLimit: Long
) : GameEntity(colorCells) {

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit): ViewUnit = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.SQUARE)
        }
    }

    override val physicalEntity = object : PhysicalEntity() {
        override val isSolid = false
        override fun getUnitDirection(unit: GameUnit, dir: Direction): Direction = dir
    }

    override val fightEntity = object : FightEntity() {
        override fun isUnitActive(unit: GameUnit): Boolean = true
    }

    override fun onLifeStart() {
        controller.view.receive(View.SubscribeToMovement(this))
        controller.view.receive(View.SubscribeToInventory(this))
    }

    override fun onLifeEnd() {
        controller.view.receive(View.UnsubscribeFromInventory(this))
        controller.view.receive(View.UnsubscribeFromMovement(this))
    }

    private val inventory: Inventory = Inventory(invDesc.invGridW, invDesc.invGridH)

    // automaton start

    private abstract inner class HeroState {
        abstract suspend fun next(m: Message): HeroState
    }

    private inner class PlayingState : HeroState() {

        private var lastMoveTime = System.currentTimeMillis()
        private lateinit var lastMoveDir: Direction

        override suspend fun next(m: Message) = when (m) {
            is UserMoved -> this.also {
                val curMoveTime = System.currentTimeMillis()
                if (!::lastMoveDir.isInitialized) lastMoveDir = m.dir
                if (lastMoveDir == m.dir && curMoveTime - lastMoveTime <= singleDirMovePeriodLimit) return@also
                lastMoveTime = curMoveTime
                lastMoveDir = m.dir

                val moved = controller.physics.tryMove(this@Hero, m.dir)
                if (moved) controller.view.receive(EntityUpdated(this@Hero))
            }
            is CollidedWith -> this.also {
                controller.fighting.attack(m.myUnit, m.otherUnit)
                // TODO: pick item instead of attacking
            }
            is ColorTick -> this.also {
                controller.fighting.update(m.unit, ControlParams(AttackType.RANDOM_TARGET, HealType.NO_HEAL))
            }
            is UserToggledInventory -> InventoryState().also {
                controller.view.receive(InventoryOpened(inventory))
            }
            is ReceivedAttack -> this.also {
                if (m.isFatal) {
                    controller.creation.die(this@Hero)
                    controller.receive(FinishControllerMessage(false))
                }
            }
            else -> {
                println(m)
                unreachable
            }
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
            is CollidedWith -> this.also {
                controller.fighting.attack(m.myUnit, m.otherUnit)
            }
            is ColorTick -> this.also {
                controller.fighting.update(m.unit, ControlParams(AttackType.RANDOM_TARGET, HealType.NO_HEAL))
            }
            is ReceivedAttack -> {
                if (m.isFatal) {
                    controller.view.receive(InventoryClosed())
                    controller.creation.die(this@Hero)
                    controller.receive(FinishControllerMessage(false))
                    PlayingState()
                } else {
                    this
                }
            }
            else -> {
                println(m)
                unreachable
            }
        }
    }

    private var state: HeroState = PlayingState()

    // automaton end

    override suspend fun handleGameMessage(m: Message) {
        state = state.next(m)
    }

}
