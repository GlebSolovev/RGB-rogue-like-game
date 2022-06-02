package ru.hse.sd.rgb.utils.messaging

import kotlinx.coroutines.*
import ru.hse.sd.rgb.utils.ConcurrentHashSet
import ru.hse.sd.rgb.utils.getValue
import ru.hse.sd.rgb.utils.setValue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

open class Tick : Message()

class Ticker(
    periodMillis: Long,
    private val target: Messagable,
    val tick: Tick,
) {
    private lateinit var coroutineJob: Job
    var isTicking: Boolean by AtomicReference(false)

    var periodMillis: Long by AtomicReference(periodMillis)
    var periodCoef: Double by AtomicReference(1.0)

    fun start() {
        if (isTicking) return
        coroutineJob = tickerCoroutineScope.launch {
            tickingRoutine()
        }
        isTicking = true
        tickers.getOrPut(target) { ConcurrentHashSet() }.add(this)
    }

    fun stop() {
        if (!isTicking) return
        coroutineJob.cancel()
        isTicking = false
        tickers[target]!!.remove(this)
    }

    private suspend fun CoroutineScope.tickingRoutine() {
        while (isActive) {
            // TODO: recalculate delay on periodMillis update
            delay((periodMillis * periodCoef).toLong())
            target.receive(tick)
        }
    }

    companion object {
        private val tickers = ConcurrentHashMap<Messagable, MutableSet<Ticker>>()

        fun Messagable.createTicker(periodMillis: Long, tick: Tick = Tick()) = Ticker(periodMillis, this, tick)

        fun tryStopTickers(m: Messagable) {
            tickers[m]?.let { for (t in it) t.stop() }
            tickers.remove(m)
        }

        private var tickerCoroutineScope = CoroutineScope(Dispatchers.Default)

        fun resetAll() {
            tickerCoroutineScope.cancel()
            tickerCoroutineScope = CoroutineScope(Dispatchers.Default)
        }
    }
}
