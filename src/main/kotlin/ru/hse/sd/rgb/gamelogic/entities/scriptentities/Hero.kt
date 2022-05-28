package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.gameloaders.InventoryDescription
import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.SimpleBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.State
import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.engines.fight.AttackType
import ru.hse.sd.rgb.gamelogic.engines.fight.ControlParams
import ru.hse.sd.rgb.gamelogic.engines.fight.HealType
import ru.hse.sd.rgb.gamelogic.items.Inventory
import ru.hse.sd.rgb.gamelogic.entities.*
import ru.hse.sd.rgb.utils.Direction
import ru.hse.sd.rgb.utils.messaging.*
import ru.hse.sd.rgb.utils.messaging.messages.*
import ru.hse.sd.rgb.utils.sameAs
import ru.hse.sd.rgb.views.*
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class Hero(
    colorCells: Set<ColorCellHp>,
    invDesc: InventoryDescription,
    private var singleDirMovePeriodLimit: Long
) : GameEntity(colorCells) {

    override val viewEntity = object : ViewEntity() {
        private val scaleFactors = mutableMapOf<GameUnit, Double>()

        override fun convertUnit(unit: GameUnit): ViewUnit = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(
                SwingUnitShape.SQUARE,
                scaleFactors.getOrDefault(unit, 1.0)
            )
        }

        override fun applyMessageToAppearance(m: Message) {
            // TODO: allow to easily reuse this code in other GameEntities (maybe use mixins?)
            if (m is ReceivedAttack) {
                val unit = m.myUnit as HpGameUnit
                val scale = unit.hp.toDouble() / unit.maxHp
                if (scale sameAs 1.0) {
                    scaleFactors.remove(unit)
                } else {
                    scaleFactors[unit] = scale
                }
                controller.view.receive(EntityUpdated(this@Hero))
            }
        }
    }

    override val physicalEntity = object : PhysicalEntity() {
        override val isSolid = false
        override fun getUnitDirection(unit: GameUnit, dir: Direction): Direction = dir
    }

    override val fightEntity = object : FightEntity() {
        override fun isUnitActive(unit: GameUnit): Boolean = true
        override val teamId = controller.fighting.newTeamId()
    }

    override fun onLifeStart() {
        controller.view.receive(SubscribeToMovement(this))
        controller.view.receive(SubscribeToInventory(this))
    }

    override fun onLifeEnd() {
        controller.view.receive(UnsubscribeFromInventory(this))
        controller.view.receive(UnsubscribeFromMovement(this))
    }

    override var behaviour: Behaviour = HeroBehaviour()
    override val behaviourEntity = SingleBehaviourEntity(behaviour)

    private val inventory: Inventory = Inventory(invDesc.invGridW, invDesc.invGridH)

    inner class HeroBehaviour : SimpleBehaviour() {

        override var state: State = PlayingState()

        private open inner class PlayingState : State() {
            private var lastMoveTime = System.currentTimeMillis()
            private lateinit var lastMoveDir: Direction

            override suspend fun handleReceivedAttack(message: ReceivedAttack): State {
                if (message.isFatal) {
                    controller.creation.die(this@Hero)
                    controller.receive(FinishControllerMessage(false))
                }
                return this
            }

            override suspend fun handleCollidedWith(message: CollidedWith): State {
                controller.fighting.attack(message.myUnit, message.otherUnit)
                // TODO: pick item instead of attacking
                return this
            }

            override suspend fun handleUserMoved(message: UserMoved): State {
                val curMoveTime = System.currentTimeMillis()
                if (!::lastMoveDir.isInitialized) lastMoveDir = message.dir
                if (lastMoveDir == message.dir && curMoveTime - lastMoveTime <= singleDirMovePeriodLimit) return this
                lastMoveTime = curMoveTime
                lastMoveDir = message.dir

                val moved = controller.physics.tryMove(this@Hero, message.dir)
                if (moved) controller.view.receive(EntityUpdated(this@Hero))
                return this
            }

            override suspend fun handleUserToggledInventory(): State {
                controller.view.receive(InventoryOpened(inventory))
                return InventoryState()
            }

            override suspend fun handleUserSelect(): State = this

            override suspend fun handleUserDrop(): State = this

            override suspend fun handleColorTick(tick: ColorTick): State {
                controller.fighting.update(
                    tick.unit,
                    ControlParams(AttackType.LAST_MOVE_DIR, HealType.LOWEST_HP_TARGET)
                )
                // TODO: add default control params
                return this
            }

        }

        private inner class InventoryState : PlayingState() {

            private fun sendInvUpdate() = controller.view.receive(InventoryUpdated(inventory))

            override suspend fun handleUserMoved(message: UserMoved): State {
                inventory.moveSelection(message.dir)
                sendInvUpdate()
                return this
            }

            override suspend fun handleUserToggledInventory(): State {
                controller.view.receive(InventoryClosed())
                return PlayingState()
            }

            override suspend fun handleUserSelect(): State {
                inventory.useCurrent()
                sendInvUpdate()
                return this
            }

            override suspend fun handleUserDrop(): State {
                inventory.dropCurrent()
                // TODO: place item in world
                sendInvUpdate()
                return this
            }
        }
    }
}
