package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuilder
import ru.hse.sd.rgb.gamelogic.engines.behaviour.NoneBehaviour
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.messages.CollidedWith
import ru.hse.sd.rgb.utils.messaging.messages.NextLevel
import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.utils.structures.Direction
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class LevelPortal(
    cell: Cell,
    private val nextLevelDescriptionFilename: String
) : GameEntity(setOf(ColorCellNoHp(PORTAL_COLOR, cell))) {

    companion object {
        val PORTAL_COLOR = RGB(255, 255, 255)
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

    // TODO
    override val lifecycle = BehaviourBuilder.lifecycle(
        this,
        object : NoneBehaviour(this) {
            override suspend fun handleMessage(message: Message) {
                if (message is CollidedWith && message.otherUnit.parent is Hero) {
                    controller.receive(NextLevel(nextLevelDescriptionFilename))
                }
            }
        }
    ).build()
}
