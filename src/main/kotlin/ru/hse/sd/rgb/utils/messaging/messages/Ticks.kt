package ru.hse.sd.rgb.utils.messaging.messages

import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.Tick

// ticks required to be handled in Behaviour-s

data class ColorTick(val unit: GameUnit) : Tick()

class MoveTick : Tick()
class RepaintTick : Tick()
class DieTick : Tick()
class ContinueTick : Tick()
class WatcherTick : Tick()
