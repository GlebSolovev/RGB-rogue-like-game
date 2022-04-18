package ru.hse.sd.rgb

import kotlinx.coroutines.channels.Channel
import ru.hse.sd.rgb.entities.common.GameStarted

abstract class Message

abstract class Messagable {

    private val messageChannel = Channel<Message>(Channel.UNLIMITED)

    fun receive(m: Message) = messageChannel.trySend(m.also { if(m !is GameStarted && m !is Ticker.Tick) println(it) }).getOrThrow()

    suspend fun messagingRoutine() {
        for (message in messageChannel) handleMessage(message)
    }

    abstract suspend fun handleMessage(m: Message)

}
