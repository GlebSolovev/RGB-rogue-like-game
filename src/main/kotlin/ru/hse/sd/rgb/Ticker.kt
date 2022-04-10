package ru.hse.sd.rgb

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicLong

class Ticker private constructor(
    periodMillis: Long,
    private val target: Messagable,
) {
    private val periodHolder = AtomicLong(periodMillis)
    private lateinit var coroutineJob: Job

    var periodMillis: Long
        get() = periodHolder.get()
        set(value) = periodHolder.set(value)

    fun start() {
        coroutineJob = tickerCoroutineScope.launch {
            tickingRoutine()
        }
    }

    fun stop() {
        coroutineJob.cancel()
    }

    suspend fun CoroutineScope.tickingRoutine() {
        while (isActive) {
            delay(periodMillis)
            target.receive(Tick)
        }
    }

    object Tick : Message()

    companion object {
        fun Messagable.Ticker(periodMillis: Long) = Ticker(periodMillis, this)

        private val tickerCoroutineScope = CoroutineScope(Dispatchers.Default)
    }
}
