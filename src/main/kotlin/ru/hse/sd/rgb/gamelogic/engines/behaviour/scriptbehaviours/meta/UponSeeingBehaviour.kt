package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.MetaBehaviour
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.WatcherTick
import ru.hse.sd.rgb.utils.randomCell
import ru.hse.sd.rgb.utils.structures.Paths2D

class UponSeeingBehaviour(
    entity: GameEntity,
    childBehaviour: Behaviour,
    private val targetEntity: GameEntity,
    private val seeingDepth: Int,
    createSeeingBehaviour: (Behaviour) -> Behaviour
) : MetaBehaviour(entity, childBehaviour) {

    companion object {
        const val WATCH_PERIOD_MILLIS = 100L
    }

    private val watcherTick = WatcherTick() // NOTE: for === below
    private val watcherTicker = Ticker(WATCH_PERIOD_MILLIS, entity, watcherTick)

    override fun traverseTickers(onEach: (Ticker) -> Unit) {
        onEach(watcherTicker)
        currentBehaviour.traverseTickers(onEach)
    }

    override fun onStart() {
        currentBehaviour.onStart()
    }

    override fun onStop() {
        currentBehaviour.onStop()
    }

    private val seeingBehavior = createSeeingBehaviour(childBehaviour)
    private var currentBehaviour = childBehaviour

    override suspend fun handleMessage(message: Message) {
        if (message === watcherTick) {
            handleWatcherTick()
        } else {
            currentBehaviour.handleMessage(message)
        }
    }

    private enum class IsSeeingState { SEEING, NOT_SEEING }

    private var state = IsSeeingState.NOT_SEEING

    private suspend fun handleWatcherTick() {
        state = when (state) {
            IsSeeingState.SEEING -> {
                if (isSeeingTarget()) {
                    state
                } else {
                    seeingBehavior.stopSubtree()
                    currentBehaviour = childBehaviour
                    childBehaviour.startSubtree()
                    IsSeeingState.NOT_SEEING
                }
            }
            IsSeeingState.NOT_SEEING -> {
                if (isSeeingTarget()) {
                    childBehaviour.stopSubtree()
                    currentBehaviour = seeingBehavior
                    seeingBehavior.startSubtree()
                    IsSeeingState.SEEING
                } else {
                    state
                }
            }
        }
    }

    private suspend fun isSeeingTarget(): Boolean {
        val startCell = entity.units.first().cell
        val pathStrategy = Paths2D.straightLine(startCell, targetEntity.randomCell())
        return controller.physics.checkPathAvailability(
            pathStrategy,
            startCell,
            seeingDepth,
            { !it.parent.physicalEntity.isSolid },
            { it.parent == targetEntity }
        )
    }
}
