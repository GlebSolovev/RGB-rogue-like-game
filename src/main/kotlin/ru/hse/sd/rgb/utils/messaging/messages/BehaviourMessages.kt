package ru.hse.sd.rgb.utils.messaging.messages

import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.structures.Direction
import ru.hse.sd.rgb.utils.structures.RGB

// messages required to be handled in Behaviour-s

// game logic messages
data class HpChanged(val myUnit: GameUnit, val isFatal: Boolean) : SaveInNotStartedAndReplayInOngoingMessage()
data class CollidedWith(val myUnit: GameUnit, val otherUnit: GameUnit) : SaveInNotStartedAndReplayInOngoingMessage()
class DoMove : Message()
class Dying : SaveInNotStartedAndReplayInOngoingMessage() // message behaviours about dying

// messages from View
data class UserMoved(val dir: Direction) : Message()
class UserToggledInventory : Message()
class UserSelect : Message()
class UserDrop : Message()

// cosmetic messages
data class SetEffectColor(val enabled: Boolean, val color: RGB) : SaveInNotStartedAndReplayInOngoingMessage()

// message from ExperienceEngine to subscribers
data class ExperienceLevelUpdate(val entity: GameEntity, val newLevel: Int) :
    SaveInNotStartedAndReplayInOngoingMessage()
