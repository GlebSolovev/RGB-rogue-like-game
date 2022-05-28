package ru.hse.sd.rgb.utils.messaging.messages

import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.Direction
import ru.hse.sd.rgb.utils.messaging.Message

// messages required to be handled in Behaviour-s

// game logic messages
data class ReceivedAttack(val myUnit: GameUnit, val fromUnit: GameUnit, val isFatal: Boolean) : Message()
data class CollidedWith(val myUnit: GameUnit, val otherUnit: GameUnit) : Message()

// messages from View
data class UserMoved(val dir: Direction) : Message()
class UserToggledInventory : Message()
class UserSelect : Message()
class UserDrop : Message()
