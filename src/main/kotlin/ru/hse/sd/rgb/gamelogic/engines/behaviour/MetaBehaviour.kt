package ru.hse.sd.rgb.gamelogic.engines.behaviour

import ru.hse.sd.rgb.gamelogic.entities.GameEntity

abstract class MetaBehaviour(protected var behaviour: Behaviour, entity: GameEntity) : Behaviour(entity)
