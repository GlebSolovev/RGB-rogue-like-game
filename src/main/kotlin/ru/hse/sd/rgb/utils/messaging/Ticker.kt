package ru.hse.sd.rgb.utils

import kotlinx.coroutines.*
import ru.hse.sd.rgb.utils.messaging.Messagable
import ru.hse.sd.rgb.utils.messaging.Message
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

open class Tick : Message()

class Ticker(
    periodMillis: Long,
    private val target: Messagable,
    private val tick: Tick,
) {
    private val periodHolder = AtomicLong(periodMillis)
    private lateinit var coroutineJob: Job

    private var periodMillis: Long
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

    private suspend fun CoroutineScope.tickingRoutine() {
        while (isActive) {
            delay(periodMillis)
            target.receive(tick)
        }
    }

    companion object {
        private val tickers = ConcurrentHashMap<Messagable, MutableSet<Ticker>>()

        fun Messagable.createTicker(periodMillis: Long, tick: Tick = Tick()): Ticker {
            val t = Ticker(periodMillis, this, tick)
            tickers.getOrPut(this@createTicker) { ConcurrentHashSet() }.add(t)
            return t
        }

        fun tryStopTickers(m: Messagable) {
            tickers.remove(m)?.let { for (t in it) t.stop() }
        }

        private var tickerCoroutineScope = CoroutineScope(Dispatchers.Default)

        fun resetAll() {
            tickerCoroutineScope.cancel()
            tickerCoroutineScope = CoroutineScope(Dispatchers.Default)
        }
    }
}