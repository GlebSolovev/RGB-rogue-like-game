@file:Suppress("FunctionName")

package ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.meta

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.BehaviourBuilder
import ru.hse.sd.rgb.gamelogic.engines.behaviour.MetaBehaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.EnableColorUpdate
import ru.hse.sd.rgb.gamelogic.engines.behaviour.scriptbehaviours.buildingblocks.MoveDirectlyFrom
import ru.hse.sd.rgb.gamelogic.engines.fight.AttackType
import ru.hse.sd.rgb.gamelogic.engines.fight.ControlParams
import ru.hse.sd.rgb.gamelogic.engines.fight.HealType

fun DirectFleeFromHeroBehaviour(
    base: Behaviour,
    movePeriodMillis: Long,
): MetaBehaviour = BehaviourBuilder.metaFromBlocks(base)
    .add { MoveDirectlyFrom(entity, childBlock, movePeriodMillis, controller.hero) }
    .add { EnableColorUpdate(entity, childBlock, ControlParams(AttackType.NO_ATTACK, HealType.LOWEST_HP_TARGET)) }
    .build()
