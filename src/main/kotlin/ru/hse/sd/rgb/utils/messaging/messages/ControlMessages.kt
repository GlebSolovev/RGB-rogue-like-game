package ru.hse.sd.rgb.utils.messaging.messages

import ru.hse.sd.rgb.gamelogic.engines.behaviour.Behaviour
import ru.hse.sd.rgb.gamelogic.engines.behaviour.MetaBehaviour
import ru.hse.sd.rgb.utils.messaging.Message

// global control messages

// for Controller
class StartControllerMessage : Message()
data class FinishControllerMessage(val isWin: Boolean) : Message()
class UserQuit : Message()
data class NextLevel(val nextLevelDescriptionFilename: String) : Message()
class DoLoadLevel : Message()

// for LifecycleBehaviour
class LifeStarted : Message()
data class LifeEnded(val dieRoutine: suspend () -> Unit) : Message()

data class ApplyBehaviourMessage(val createNewBehaviour: (Behaviour) -> Behaviour) :
    SaveInNotStartedAndReplayInOngoingMessage()

data class RemoveBehaviourMessage(val target: MetaBehaviour) : SaveInNotStartedAndReplayInOngoingMessage()
