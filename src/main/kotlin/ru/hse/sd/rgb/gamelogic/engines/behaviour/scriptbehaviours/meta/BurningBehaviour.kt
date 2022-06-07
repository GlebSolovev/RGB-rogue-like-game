package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.MetaBehaviour
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.BurnTick
import ru.hse.sd.rgb.utils.messaging.messages.CollidedWith
import ru.hse.sd.rgb.utils.messaging.messages.SetEffectColor
import ru.hse.sd.rgb.utils.structures.RGB

class BurningBehaviour(
    entity: GameEntity,
    childBehaviour: Behaviour,
    private val attackPeriodMillis: Long,
    private val attack: Int,
    private val initialDurationMillis: Long?
) : MetaBehaviour(entity, childBehaviour) {

    companion object {
        val BURNING_EFFECT_COLOR = RGB(220, 30, 0)
        const val SPREAD_BURNINGS_DURATION_COEFFICIENT = 0.5
        const val SPREAD_BURNINGS_ENABLE = false // TODO: fight stack overflow if enabled
    }

    private val burnTick = BurnTick()
    private val burningTicker = Ticker(attackPeriodMillis, entity, burnTick)

    override fun onStart() {
        entity.receive(SetEffectColor(true, BURNING_EFFECT_COLOR))
    }

    override fun onStop() {
        entity.receive(SetEffectColor(false, BURNING_EFFECT_COLOR))
    }

    override suspend fun handleMessage(message: Message) {
        when (message) {
            burnTick -> controller.fighting.attackDirectly(entity.units.random(), attack)
            is CollidedWith -> {
                if (SPREAD_BURNINGS_ENABLE) {
                    controller.behaviourEngine.applyBurningBehaviour(
                        message.otherUnit.parent,
                        attackPeriodMillis,
                        attack,
                        if (initialDurationMillis == null) null
                        else (initialDurationMillis * SPREAD_BURNINGS_DURATION_COEFFICIENT).toLong()
                    )
                }
                childBehaviour.handleMessage(message)
            }
            else -> childBehaviour.handleMessage(message)
        }
    }

    override fun traverseTickers(onEach: (Ticker) -> Unit) = onEach(burningTicker)
}
