package ru.hse.sd.rgb.gamelogic.entities.scriptentities

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuilder
import ru.hse.sd.rgb.gamelogic.engines.behaviour.NoneBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.AnnihilateItems
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.AttackOnCollision
import ru.hse.sd.rgb.gamelogic.entities.ColorCellNoHp
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.*
import ru.hse.sd.rgb.utils.structures.Direction
import ru.hse.sd.rgb.views.ViewUnit
import ru.hse.sd.rgb.views.swing.SwingUnitAppearance
import ru.hse.sd.rgb.views.swing.SwingUnitShape

class LaserPart(
    cell: ColorCellNoHp,
    private val persistMillis: Long,
    private val dir: Direction,
    private val teamId: Int,
) : GameEntity(setOf(cell)) {

    companion object {
        const val PROPAGATION_PERIOD_MILLIS = 6L
    }

    override val viewEntity = object : ViewEntity() {
        override fun convertUnit(unit: GameUnit) = object : ViewUnit(unit) {
            override val swingAppearance = SwingUnitAppearance(SwingUnitShape.RECTANGLE(dir), outlineColor)
        }
    }

    override val physicalEntity = object : PhysicalEntity() {
        override val isSolid = false
        override fun getUnitDirection(unit: GameUnit, dir: Direction) = dir
    }

    override val fightEntity = object : FightEntity() {
        override fun isUnitActive(unit: GameUnit) = false
        override val teamId = this@LaserPart.teamId
    }

    private val laserPartDefaultBehaviour = LaserPartDefaultBehaviour()
    override val lifecycle = BehaviourBuilder.lifecycle(this, laserPartDefaultBehaviour)
        .addBlocks {
            add { AttackOnCollision(entity, childBlock) }
            add { AnnihilateItems(entity, childBlock) }
        }.build()
    override val behaviourEntity = SingleBehaviourEntity(laserPartDefaultBehaviour)

    override val experienceEntity = object : ExperienceEntity() {
        override val onDieExperiencePoints: Int? = null
    }

    private inner class LaserPartDefaultBehaviour : NoneBehaviour(this) {

        private val continueTicker = Ticker(PROPAGATION_PERIOD_MILLIS, this@LaserPart, ContinueTick())
        private val dieTicker = Ticker(persistMillis, this@LaserPart, DieTick())
        private var didContinue = false
        private var didDie = false

        override fun traverseTickers(onEach: (Ticker) -> Unit) {
            onEach(continueTicker)
            onEach(dieTicker)
        }

        override suspend fun handleMessage(message: Message) {
            when (message) {
                is DoMove -> {
                    if (controller.physics.tryMove(this@LaserPart, dir))
                        controller.view.receive(EntityUpdated(this@LaserPart))
                    else
                        didContinue = true // prevent stuck parts from cloning
                }
                is DieTick -> {
                    if (!didDie) {
                        continueTicker.stop()
                        dieTicker.stop()
                        controller.creation.die(this@LaserPart)
                        didDie = true
                    }
                }
                is ContinueTick -> {
                    if (!didContinue) {
                        val clone = clone()
                        if (controller.creation.tryAddToWorld(clone)) {
                            controller.view.receive(EntityUpdated(clone))
                            clone.receive(DoMove())
                        }
                        didContinue = true
                        continueTicker.stop()
                    }
                }
            }
        }
    }

    private fun clone(): LaserPart {
        val unit = units.first()
        return LaserPart(ColorCellNoHp(unit.gameColor, unit.cell), persistMillis, dir, teamId)
    }
}
