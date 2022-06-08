package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.*
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta.BurningBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta.ConfusedBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta.FrozenBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta.MultiplySpeedBehaviour
import ru.hse.sd.rgb.gamelogic.engines.experience.Experience
import ru.hse.sd.rgb.gamelogic.engines.fight.AttackType
import ru.hse.sd.rgb.gamelogic.engines.fight.ControlParams
import ru.hse.sd.rgb.gamelogic.engines.fight.HealType
import ru.hse.sd.rgb.gamelogic.engines.items.Inventory
import ru.hse.sd.rgb.gamelogic.engines.items.InventoryPersistence
import ru.hse.sd.rgb.gamelogic.engines.items.ItemEntity
import ru.hse.sd.rgb.gamelogic.entities.ColorCellHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.gamelogic.entities.HpGameUnit
import ru.hse.sd.rgb.utils.ignore
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.*
import ru.hse.sd.rgb.utils.randomCell
import ru.hse.sd.rgb.utils.sameAs
import ru.hse.sd.rgb.utils.structures.*
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

data class HeroPersistence(
    val unitsPersistence: List<HpUnitPersistence>,
    val inventoryPersistence: InventoryPersistence,
    val singleDirMovePeriodLimit: Long,
    val experience: Experience
) {
    data class HpUnitPersistence(
        val relativeShift: GridShift,
        val color: RGB,
        val hp: Int,
        val maxHp: Int,
    ) {
        fun convertToColorCellHp(firstCell: Cell): ColorCellHp = ColorCellHp(
            color, firstCell + relativeShift, hp, maxHp
        )
    }
}

class Hero(
    spawnCell: Cell,
    private val heroPersistence: HeroPersistence,
) : GameEntity(
    run { heroPersistence.unitsPersistence.map { it.convertToColorCellHp(spawnCell) }.toSet() }
) {

    companion object {
        const val DEFAULT_VIEW_ENTITY_SWING_SCALE_FACTOR = 1.0
        const val ADDITIONAL_FROZEN_SLOW_DOWN_COEFFICIENT = 0.5

        const val INVENTORY_VIEW_UPDATE_PERIOD_MILLIS = 10L
    }

    private var singleDirMovePeriodLimit = heroPersistence.singleDirMovePeriodLimit

    override val viewEntity: ViewEntity = HeroViewEntity()

    private inner class HeroViewEntity : ViewEntity() {
        private val swingScaleFactors = mutableMapOf<GameUnit, Double>()

        override fun convertUnit(unit: GameUnit): ViewUnit = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(
                SwingUnitShape.SQUARE,
                outlineColor,
                swingScaleFactors.getOrDefault(unit, DEFAULT_VIEW_ENTITY_SWING_SCALE_FACTOR)
            )
        }

        override fun applyMessageToAppearance(m: Message) {
            super.applyMessageToAppearance(m)
            // TODO: allow to easily reuse this code in other GameEntities
            when (m) {
                is HpChanged -> calculateScaleHp(m.myUnit as HpGameUnit)
            }
        }

        fun calculateScaleHp(unit: HpGameUnit) {
            val scale = unit.hp.toDouble() / unit.maxHp
            if (scale.sameAs(DEFAULT_VIEW_ENTITY_SWING_SCALE_FACTOR) ||
                scale > DEFAULT_VIEW_ENTITY_SWING_SCALE_FACTOR
            ) {
                swingScaleFactors.remove(unit)
            } else {
                swingScaleFactors[unit] = scale
            }
            controller.view.receive(EntityUpdated(this@Hero))
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

    override suspend fun onLifeStart() {
        controller.view.receive(SubscribeToMovement(this))
        controller.view.receive(SubscribeToInventory(this))
        for (unit in units) (viewEntity as HeroViewEntity).calculateScaleHp(unit as HpGameUnit)

        controller.experience.registerEntity(this, heroPersistence.experience)
    }

    override suspend fun onLifeEnd() {
        controller.experience.unregisterEntity(this)

        controller.view.receive(UnsubscribeFromInventory(this))
        controller.view.receive(UnsubscribeFromMovement(this))
    }

    private val heroBehaviour = HeroBehaviour()
    override val lifecycle = BehaviourBuilder.lifecycle(this, heroBehaviour).build()

    override val behaviourEntity = object : SingleBehaviourEntity(heroBehaviour) {
        override fun createConfusedBehaviour(childBehaviour: Behaviour): Behaviour =
            object : MetaBehaviour(this@Hero, childBehaviour) {
                override suspend fun handleMessage(message: Message) {
                    when (message) {
                        is UserMoved -> {
                            childBehaviour.handleMessage(UserMoved(message.dir.opposite()))
                        }
                        is ColorTick -> controller.fighting.update(
                            message.unit,
                            ControlParams(AttackType.RANDOM_TARGET, HealType.RANDOM_TARGET)
                        )
                        else -> childBehaviour.handleMessage(message)
                    }
                }

                override fun onStart() {
                    entity.receive(SetEffectColor(true, ConfusedBehaviour.CONFUSED_EFFECT_COLOR))
                }

                override fun onStop() {
                    entity.receive(SetEffectColor(false, ConfusedBehaviour.CONFUSED_EFFECT_COLOR))
                }

                override fun traverseTickers(onEach: (Ticker) -> Unit) = ignore
            }

        override fun createBurningBehaviour(
            childBehaviour: Behaviour,
            attackPeriodMillis: Long,
            attack: Int,
            initialDurationMillis: Long?
        ): Behaviour {
            return BurningBehaviour(
                this@Hero,
                childBehaviour,
                attackPeriodMillis,
                attack,
                initialDurationMillis
            )
        }

        override fun createFrozenBehaviour(childBehaviour: Behaviour, slowDownCoefficient: Double): Behaviour =
            object : MultiplySpeedBehaviour(this@Hero, childBehaviour, slowDownCoefficient) {
                private val totalSlowDownCoefficient = slowDownCoefficient * ADDITIONAL_FROZEN_SLOW_DOWN_COEFFICIENT

                override fun onStart() {
                    super.onStart()
                    entity.receive(SetEffectColor(true, FrozenBehaviour.FROZEN_EFFECT_COLOR))
                    singleDirMovePeriodLimit = (singleDirMovePeriodLimit / totalSlowDownCoefficient).toLong()
                }

                override fun onStop() {
                    super.onStop()
                    entity.receive(SetEffectColor(false, FrozenBehaviour.FROZEN_EFFECT_COLOR))
                    singleDirMovePeriodLimit = (singleDirMovePeriodLimit * totalSlowDownCoefficient).toLong()
                }
            }
    }

    override val experienceEntity = object : ExperienceEntity() {
        override val onDieExperiencePoints: Int? = null
    }

    private val inventory: Inventory = heroPersistence.inventoryPersistence.convertToInventory(this)

    private inner class HeroBehaviour : NoneBehaviour(this) {

        private var state: State = PlayingState()

        override suspend fun handleMessage(message: Message) {
            state = state.next(message)
        }

        private val inventoryViewUpdateTicker =
            Ticker(INVENTORY_VIEW_UPDATE_PERIOD_MILLIS, this@Hero, DoUpdateInventoryViewTick())

        override fun traverseTickers(onEach: (Ticker) -> Unit) = onEach(inventoryViewUpdateTicker)

        override fun onStart() {
            // prevent unnecessary inventory ticker start
            if (state !is InventoryState) inventoryViewUpdateTicker.stop()
        }

        @Suppress("TooManyFunctions")
        private open inner class PlayingState : State() {
            private var lastMoveTime = System.currentTimeMillis()
            private lateinit var lastMoveDir: Direction

            override suspend fun handleHpChanged(message: HpChanged): State {
                if (message.isFatal) {
                    controller.creation.die(this@Hero)
                }
                return this
            }

            override suspend fun handleCollidedWith(message: CollidedWith): State {
                val collidedWithEntity = message.otherUnit.parent
                if (collidedWithEntity is ItemEntity) {
                    if (inventory.isNotFull()) {
                        val item = controller.itemsEngine.tryPick(this@Hero, collidedWithEntity) ?: return this
                        inventory.addItem(item)
                    }
                } else {
                    controller.fighting.attack(message.myUnit, message.otherUnit)
                }
                return this
            }

            @Suppress("MagicNumber")
            override suspend fun handleDying(): State {
                val unitList = units.toList()
                val first = unitList.first()
                val unitsPersistence = unitList.map {
                    it as HpGameUnit
                    HeroPersistence.HpUnitPersistence(first.cell - it.cell, it.gameColor, 1, 1)
                }
                val fakePersistence = HeroPersistence(
                    unitsPersistence,
                    inventory.extractPersistence(),
                    50, // TODO: for some reason only works when hardcoded
                    controller.experience.getExperience(this@Hero)!!
                )
                controller.receive(
                    ControllerNextLevel(controller.loseLevelDescriptionFilename, fakePersistence)
                )
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
                controller.view.receive(InventoryOpened.fromInventory(inventory))
                inventoryViewUpdateTicker.start()
                return InventoryState()
            }

            override suspend fun handleUserSelect(): State = this

            override suspend fun handleUserDrop(): State = this

            override suspend fun handleSetEffectColor(message: SetEffectColor): State = this

            override suspend fun handleColorTick(tick: ColorTick): State {
                controller.fighting.update(
                    tick.unit,
                    ControlParams(AttackType.LAST_MOVE_DIR, HealType.LOWEST_HP_TARGET)
                )
                // TODO: add default control params
                return this
            }

            override suspend fun handleDoUpdateInventoryViewTick(): State = this

            override suspend fun handleHeroNextLevel(message: HeroNextLevel): State {
                controller.receive(
                    ControllerNextLevel(message.nextLevelDescriptionFilename, extractPersistence())
                )
                return this
            }
        }

        private inner class InventoryState : PlayingState() {

            // InventoryUpdated is regularly sent to View by inventoryViewUpdateTicker automatically

            override suspend fun handleUserMoved(message: UserMoved): State {
                inventory.moveSelection(message.dir)
                return this
            }

            override suspend fun handleUserToggledInventory(): State {
                controller.view.receive(InventoryClosed())
                inventoryViewUpdateTicker.stop()
                return PlayingState()
            }

            override suspend fun handleUserSelect(): State {
                inventory.useCurrent()
                return this
            }

            override suspend fun handleUserDrop(): State {
                inventory.dropCurrent(randomCell())
                // TODO: if false show message "Can't drop item, try again"
                return this
            }

            override suspend fun handleDoUpdateInventoryViewTick(): State {
                controller.view.receive(InventoryUpdated.fromInventory(inventory))
                return this
            }
        }
    }

    private suspend fun extractPersistence(): HeroPersistence {
        val unitList = units.toList()
        val first = unitList.first()
        val unitsPersistence = unitList.map {
            it as HpGameUnit
            HeroPersistence.HpUnitPersistence(first.cell - it.cell, it.gameColor, it.hp, it.maxHp)
        }
        return HeroPersistence(
            unitsPersistence,
            inventory.extractPersistence(),
            singleDirMovePeriodLimit,
            controller.experience.getExperience(this)!!
        )
    }
}
