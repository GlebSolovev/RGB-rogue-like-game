package ru.hse.sd.rgb.utils.messaging.messages

import ru.hse.sd.rgb.utils.messaging.Message

// global control messages

// for Controller
class StartControllerMessage : Message()
data class FinishControllerMessage(val isWin: Boolean) : Message()
class UserQuit : Message()

// for Entity
class LifeStarted : Message()
data class LifeEnded(val dieRoutine: suspend () -> Unit) : Message()
