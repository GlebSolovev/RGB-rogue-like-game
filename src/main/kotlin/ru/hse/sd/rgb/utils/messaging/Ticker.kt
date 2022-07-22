package ru.hse.sd.rgb.utils.messaging

import ru.hse.sd.rgb.utils.ConcurrentHashSet
import ru.hse.sd.rgb.utils.getValue
import ru.hse.sd.rgb.utils.setValue
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

open class Tick : Message()

open class Ticker(
    periodMillis: Long,
    protected val target: Messagable,
    val tick: Tick,
    protected val scope: CoroutineScope = tickerCoroutineScope,
) {
    private lateinit var coroutineJob: Job
    var isTicking: Boolean by AtomicReference(false)

    var periodMillis: Long by AtomicReference(periodMillis)
    var periodCoefficient: Double by AtomicReference(1.0)

    open fun start() {
        if (isTicking) return
        coroutineJob = scope.launch {
            tickingRoutine()
        }
        isTicking = true
        tickers.getOrPut(target) { ConcurrentHashSet() }.add(this)
    }

    fun stop() {
        if (!isTicking) return
        coroutineJob.cancel()
        isTicking = false
        tickers[target]?.remove(this)
    }

    protected open suspend fun CoroutineScope.tickingRoutine() {
        while (isActive) {
            // TODO: recalculate delay on periodMillis update
            delay((periodMillis * periodCoefficient).toLong())
            target.receive(tick)
        }
    }

    companion object {
        private val tickers = ConcurrentHashMap<Messagable, MutableSet<Ticker>>()

        private var tickerCoroutineScope = CoroutineScope(Dispatchers.Default)

        fun Messagable.createTicker(
            periodMillis: Long,
            tick: Tick = Tick(),
            scope: CoroutineScope = tickerCoroutineScope,
        ) = Ticker(periodMillis, this, tick, scope)

        fun tryStopTickers(m: Messagable) {
            tickers[m]?.let { for (t in it) t.stop() }
            tickers.remove(m)
        }

        fun stopDefaultScope() {
            tickerCoroutineScope.cancel()
            tickerCoroutineScope = CoroutineScope(Dispatchers.Default)
        }
    }
}

class OneTimeTicker(periodMillis: Long, target: Messagable, tick: Tick) : Ticker(periodMillis, target, tick) {
    private var flag by AtomicReference(false)

    override fun start() {
        if (!flag) {
            flag = true
            super.start()
        }
    }

    override suspend fun CoroutineScope.tickingRoutine() {
        delay((periodMillis * periodCoefficient).toLong())
        target.receive(tick)
        stop()
    }
}
