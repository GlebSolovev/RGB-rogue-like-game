package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.MetaBehaviour
import ru.hse.sd.rgb.gamelogic.engines.fight.AttackType
import ru.hse.sd.rgb.gamelogic.engines.fight.ControlParams
import ru.hse.sd.rgb.gamelogic.engines.fight.HealType
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.ignore
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.ColorTick
import ru.hse.sd.rgb.utils.messaging.messages.EntityUpdated
import ru.hse.sd.rgb.utils.messaging.messages.MoveTick
import ru.hse.sd.rgb.utils.messaging.messages.SetEffectColor
import ru.hse.sd.rgb.utils.structures.Direction
import ru.hse.sd.rgb.utils.structures.RGB

class ConfusedBehaviour(entity: GameEntity, childBehaviour: Behaviour) : MetaBehaviour(entity, childBehaviour) {

    companion object {
        val CONFUSED_EFFECT_COLOR = RGB(200, 60, 200)
    }

    override fun onStart() {
        entity.receive(SetEffectColor(true, CONFUSED_EFFECT_COLOR))
    }

    override fun onStop() {
        entity.receive(SetEffectColor(false, CONFUSED_EFFECT_COLOR))
    }

    override suspend fun handleMessage(message: Message) {
        when (message) {
            is MoveTick -> {
                val dir = Direction.random()
                val moved = controller.physics.tryMove(entity, dir)
                if (moved) controller.view.receive(EntityUpdated(entity))
            }
            is ColorTick -> controller.fighting.update(
                message.unit,
                ControlParams(AttackType.RANDOM_TARGET, HealType.RANDOM_TARGET)
            )
            else -> childBehaviour.handleMessage(message)
        }
    }

    override fun traverseTickers(onEach: (Ticker) -> Unit) = ignore
}
