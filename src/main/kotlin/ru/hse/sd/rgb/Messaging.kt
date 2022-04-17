package ru.hse.sd.rgb

import kotlinx.coroutines.channels.Channel

abstract class Message

abstract class Messagable {

    private val messageChannel = Channel<Message>(Channel.UNLIMITED) // TODO: magic number

    fun receive(m: Message) = messageChannel.trySend(m).getOrThrow()

    suspend fun messagingRoutine() {
        for (message in messageChannel) handleMessage(message)
    }

    abstract suspend fun handleMessage(m: Message)

}
