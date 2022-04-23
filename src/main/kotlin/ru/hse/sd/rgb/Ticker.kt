package ru.hse.sd.rgb

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicLong

open class Tick : Message()

class Ticker(
    periodMillis: Long,
    private val target: Messagable,
    private val tick: Tick,
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
            target.receive(tick)
        }
    }


    companion object {
        fun Messagable.Ticker(periodMillis: Long, tick: Tick = Tick()) = Ticker(periodMillis, this, tick)

        private var tickerCoroutineScope = CoroutineScope(Dispatchers.Default)

        fun resetAll() {
            tickerCoroutineScope.cancel()
            tickerCoroutineScope = CoroutineScope(Dispatchers.Default)
        }
    }
}
