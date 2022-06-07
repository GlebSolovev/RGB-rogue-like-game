package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta

import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.MetaBehaviour
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.ignore
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.ColorTick
import ru.hse.sd.rgb.utils.messaging.messages.MoveTick
import ru.hse.sd.rgb.utils.messaging.messages.SetEffectColor
import ru.hse.sd.rgb.utils.structures.RGB

open class MultiplySpeedBehaviour(
    entity: GameEntity,
    childBehaviour: Behaviour,
    private val speedCoefficient: Double // > 1.0 = increase speed, < 1.0 = decrease speed
) : MetaBehaviour(entity, childBehaviour) {

    private fun getTickersToModify(): Set<Ticker> =
        childBehaviour.tickersGroup(MoveTick::class) + childBehaviour.tickersGroup(ColorTick::class)

    override fun onStart() {
        getTickersToModify().forEach { it.periodCoefficient /= speedCoefficient }
    }

    override fun onStop() {
        getTickersToModify().forEach { it.periodCoefficient *= speedCoefficient }
    }

    override suspend fun handleMessage(message: Message) = childBehaviour.handleMessage(message)

    override fun traverseTickers(onEach: (Ticker) -> Unit) = ignore
}

class FrozenBehaviour(
    entity: GameEntity,
    childBehaviour: Behaviour,
    slowDownCoefficient: Double
) : MultiplySpeedBehaviour(entity, childBehaviour, slowDownCoefficient) {

    companion object {
        val FROZEN_EFFECT_COLOR = RGB(220, 240, 250)
    }

    init {
        if (slowDownCoefficient >= 1.0) throw IllegalArgumentException("speedCoefficient has to be < 1.0")
    }

    override fun onStart() {
        super.onStart()
        entity.receive(SetEffectColor(true, FROZEN_EFFECT_COLOR))
    }

    override fun onStop() {
        super.onStop()
        entity.receive(SetEffectColor(false, FROZEN_EFFECT_COLOR))
    }
}
