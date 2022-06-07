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

        const val SPREAD_BURNING_PERIOD_LIMIT_MILLIS = 2000
        const val SPREAD_BURNING_DURATION_COEFFICIENT = 0.8
    }

    private val burnTick = BurnTick()
    private val burningTicker = Ticker(attackPeriodMillis, entity, burnTick)

    private var lastSpreadBurningMillis = System.currentTimeMillis()

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
                val currentMillis = System.currentTimeMillis()
                if (currentMillis - lastSpreadBurningMillis >= SPREAD_BURNING_PERIOD_LIMIT_MILLIS) {
                    controller.behaviourEngine.applyBurningBehaviour(
                        message.otherUnit.parent,
                        attackPeriodMillis,
                        attack,
                        if (initialDurationMillis == null) null
                        else (initialDurationMillis * SPREAD_BURNING_DURATION_COEFFICIENT).toLong()
                    )
                    lastSpreadBurningMillis = currentMillis
                }
                childBehaviour.handleMessage(message)
            }
            else -> childBehaviour.handleMessage(message)
        }
    }

    override fun traverseTickers(onEach: (Ticker) -> Unit) = onEach(burningTicker)
}
