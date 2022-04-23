package ru.hse.sd.rgb.logic

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.hse.sd.rgb.GameColor
import ru.hse.sd.rgb.basecolorupdate.BaseColorUpdateEffect
import ru.hse.sd.rgb.basecolorupdate.ControlParams
import ru.hse.sd.rgb.entities.common.GameUnit
import ru.hse.sd.rgb.entities.common.GameUnitId
import ru.hse.sd.rgb.entities.common.ReceivedAttack
import ru.hse.sd.rgb.views.RGB
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

typealias BaseColorId = Int

data class BaseColorStats(
    val name: String,
    val rgb: RGB,
    val updatePeriodMillis: Long,
    val updateEffects: List<BaseColorUpdateEffect>
)

class FightLogic(
    private val baseColorStats: ConcurrentHashMap<BaseColorId, BaseColorStats>,
    private val attackFromTo: ConcurrentHashMap<Pair<BaseColorId, BaseColorId>, Int>
) {

    private val unitMutexes = ConcurrentHashMap<GameUnitId, Mutex>()
    private val unsafeMethods: UnsafeMethods = UnsafeMethodsImpl()

    interface UnsafeMethods {
        fun unsafeChangeRGB(unit: GameUnit, newRgb: RGB)
        fun unsafeAttack(from: GameUnit, to: GameUnit)
    }

    private inner class UnsafeMethodsImpl : UnsafeMethods {
        override fun unsafeChangeRGB(unit: GameUnit, newRgb: RGB) {
            unit.gameColor.rgb = newRgb
            unit.gameColor.cachedBaseColorId = resolveBaseColor(unit.gameColor.rgb)
        }

        override fun unsafeAttack(from: GameUnit, to: GameUnit) {
            to.hp -= computeAttack(from.gameColor, to.gameColor)
            to.parent.receive(ReceivedAttack(to, from, to.hp < 0))
        }

    }

    private suspend inline fun <R> withLockedUnits(units: Set<GameUnit>, crossinline block: () -> R): R {
        val sortedUnits = units.toSortedSet(Comparator.comparing(GameUnit::id))
        try {
            for (unit in sortedUnits) unitMutexes[unit.id]!!.lock()
            return block()
        } finally {
            for (unit in sortedUnits.reversed()) unitMutexes[unit.id]!!.unlock()
        }
    }

    private infix fun RGB.l1Norm(otherRgb: RGB): Int {
        val (r1, g1, b1) = this
        val (r2, g2, b2) = otherRgb
        return abs(r1 - r2) + abs(g1 - g2) + abs(b1 - b2)
    }

    private infix fun RGB.similarityTo(baseColorId: BaseColorId): Double {
        val baseColorRGB = baseColorId.stats.rgb
        val (r2, g2, b2) = baseColorRGB
        return (this l1Norm baseColorRGB).toDouble() / (r2 + g2 + b2)
    }

    private fun computeAttack(from: GameColor, to: GameColor): Int {
        val similarityCoef = from.rgb similarityTo to.cachedBaseColorId
        return (attackFromTo[from.cachedBaseColorId to to.cachedBaseColorId]!! * similarityCoef).toInt()
    }

    suspend fun registerUnit(unit: GameUnit) {
        val mutex = Mutex()
        mutex.withLock { unitMutexes.put(unit.id, mutex)?.let { throw IllegalStateException("double unit register") } }
    }

    suspend fun unregisterUnit(unit: GameUnit) {
        val mutex = unitMutexes[unit.id]!!
        mutex.withLock {
            unitMutexes.remove(unit.id)
        }
    }

    fun resolveBaseColor(rgb: RGB): BaseColorId =
        baseColorStats.minWithOrNull(Comparator.comparing { it.value.rgb l1Norm rgb })!!.key

    suspend fun changeRGB(unit: GameUnit, newRgb: RGB) {
        withLockedUnits(setOf(unit)) {
            unsafeMethods.unsafeChangeRGB(unit, newRgb)
        }
    }

    suspend fun attack(from: GameUnit, to: GameUnit) {
        withLockedUnits(setOf(from, to)) {
            unsafeMethods.unsafeAttack(from, to)
        }
    }

    suspend fun update(unit: GameUnit, controlParams: ControlParams) {
        withLockedUnits(setOf(unit)) {
            for (updateEffect in unit.gameColor.cachedBaseColorId.stats.updateEffects) updateEffect.activate(
                unit,
                controlParams,
                unsafeMethods
            )
        }
    }

    fun getBaseColorStats(baseColorId: BaseColorId): BaseColorStats = baseColorId.stats

    private val BaseColorId.stats
        get() = baseColorStats[this]!!
}

