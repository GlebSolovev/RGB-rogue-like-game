package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuilder
import ru.hse.sd.rgb.gamelogic.engines.behaviour.NoneBehaviour
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.messages.CollidedWith
import ru.hse.sd.rgb.utils.messaging.messages.EntityUpdated
import ru.hse.sd.rgb.utils.messaging.messages.ExperienceLevelUpdate
import ru.hse.sd.rgb.utils.messaging.messages.NextLevel
import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.utils.structures.Direction
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class LevelPortal(
    cell: Cell,
    private val nextLevelDescriptionFilename: String,
    private val heroExperienceLevelToEnableOn: Int
) : GameEntity(setOf(ColorCellNoHp(DISABLED_PORTAL_COLOR, cell))) {

    companion object {
        val DISABLED_PORTAL_COLOR = RGB(255, 190, 190)
        val ENABLED_PORTAL_COLOR = RGB(255, 255, 255)
    }

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit) = object : ViewUnit(unit) {
            // TODO: star-5 or concentric-spinning-squares
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.CIRCLE, null)
        }
    }

    override val physicalEntity = object : PhysicalEntity() {
        override val isSolid = false
        override fun getUnitDirection(unit: GameUnit, dir: Direction) = dir
    }

    override val fightEntity = object : FightEntity() {
        override fun isUnitActive(unit: GameUnit) = false
        override val teamId = controller.fighting.newTeamId()
    }

    // TODO
    override val behaviourEntity: BehaviourEntity = BehaviourEntity()

    override val experienceEntity = object : ExperienceEntity() {
        override val onDieExperiencePoints: Int? = null
    }

    override suspend fun onLifeStart() {
        controller.experience.subscribeToExperienceLevelUpdate(this, controller.hero)
    }

    override suspend fun onLifeEnd() {
        controller.experience.unsubscribeFromExperienceLevelUpdate(this, controller.hero)
    }

    // TODO
    override val lifecycle = BehaviourBuilder.lifecycle(
        this,
        object : NoneBehaviour(this) {

            private var isEnabled = false

            override suspend fun handleMessage(message: Message) {
                when (message) {
                    is ExperienceLevelUpdate -> {
                        if (message.newLevel >= heroExperienceLevelToEnableOn && !isEnabled) {
                            isEnabled = true
                            controller.fighting.changeRGB(units.first(), ENABLED_PORTAL_COLOR)
                            controller.view.receive(EntityUpdated(this@LevelPortal))
                        }
                    }
                    is CollidedWith -> {
                        if (isEnabled && message.otherUnit.parent is Hero) {
                            controller.receive(NextLevel(nextLevelDescriptionFilename))
                        }
                    }
                }
            }
        }
    ).build()
}
