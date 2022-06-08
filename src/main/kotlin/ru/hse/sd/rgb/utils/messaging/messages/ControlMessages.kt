package ru.hse.sd.rgb.utils.messaging.messages

import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.MetaBehaviour
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.HeroPersistence
import ru.hse.sd.rgb.utils.messaging.Message

// global control messages

// for Controller
class StartControllerMessage : Message()
data class FinishControllerMessage(val isWin: Boolean) : Message()
class UserQuit : Message()
class DoLoadLevel : Message()
data class ControllerNextLevel(
    val nextLevelDescriptionFilename: String,
    val heroPersistence: HeroPersistence,
) : Message()

// for LifecycleBehaviour
class LifeStarted : Message()
data class LifeEnded(val dieRoutine: suspend () -> Unit) : Message()

data class ApplyBehaviourMessage(val createNewBehaviour: (Behaviour) -> Behaviour) :
    SaveInNotStartedAndReplayInOngoingMessage()

data class RemoveBehaviourMessage(val target: MetaBehaviour) : SaveInNotStartedAndReplayInOngoingMessage()

// for Hero
data class HeroNextLevel(val nextLevelDescriptionFilename: String) : Message()
