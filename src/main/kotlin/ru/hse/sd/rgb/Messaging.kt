package ru.hse.sd.rgb

import kotlinx.coroutines.flow.MutableSharedFlow

abstract class Message

abstract class Messagable {

    private val msgFlow = MutableSharedFlow<Message>(100) // TODO: magic number

    fun receive(m: Message) = msgFlow.tryEmit(m).takeUnless { false } ?: throw GameError("flow overflow")

    suspend fun messagingRoutine() {
        msgFlow.collect { m -> handleMessage(m) }
    }

    abstract suspend fun handleMessage(m: Message)

}
