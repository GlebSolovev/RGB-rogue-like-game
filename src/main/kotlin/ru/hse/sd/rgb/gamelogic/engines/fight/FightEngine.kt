package ru.hse.sd.rgb.gamelogic.engines.fight

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.gamelogic.entities.GameUnitId
import ru.hse.sd.rgb.gamelogic.entities.HpGameUnit
import ru.hse.sd.rgb.gamelogic.entities.ReceivedAttack
import ru.hse.sd.rgb.utils.Grid2D
import ru.hse.sd.rgb.utils.RGB
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

typealias BaseColorId = Int
private typealias BaseColorStatsMap = ConcurrentHashMap<BaseColorId, BaseColorStats>
private typealias BaseColorInteractionMatrix = ConcurrentHashMap<Pair<BaseColorId, BaseColorId>, Int>

@Serializable
data class BaseColorStats(
    val name: String,
    val rgb: RGB,
    val updatePeriodMillis: Long,
    val updateEffects: List<BaseColorUpdateEffect>
)

class FightEngine(
    baseColors: List<BaseColorStats>,
    interactionMatrix: Grid2D<Int>
) {
    private val baseColorStats = BaseColorStatsMap()
    private val attackFromTo = BaseColorInteractionMatrix()

    init {
        baseColors.withIndex().associateTo(baseColorStats) { (i, c) -> Pair(i, c) }
        interactionMatrix.withCoords().associateTo(attackFromTo) { (x, y, v) -> Pair(Pair(x, y), v) }
    }

    private val unitMutexes = ConcurrentHashMap<GameUnitId, Mutex>()
    private val unitCachedBaseColorIds = ConcurrentHashMap<GameUnitId, BaseColorId>() // TODO: AtomicRef ?
    private val unsafeMethods: UnsafeMethods = UnsafeMethodsImpl()

    interface UnsafeMethods {
        fun unsafeChangeRGB(unit: GameUnit, newRgb: RGB)
        fun unsafeAttack(from: GameUnit, to: GameUnit)
        // TODO: unsafeHeal
    }

    // TODO: is it a good idea?
    private inner class UnsafeMethodsImpl : UnsafeMethods {
        override fun unsafeChangeRGB(unit: GameUnit, newRgb: RGB) {
            unit.gameColor = newRgb
            unitCachedBaseColorIds[unit.id] = resolveBaseColor(unit.gameColor)
        }

        override fun unsafeAttack(from: GameUnit, to: GameUnit) {
            val isFatal = if (to is HpGameUnit) {
                val atk = computeAttack(from, to)
                if (atk > 0 && from.parent.fightEntity.teamId == to.parent.fightEntity.teamId) return
                to.hp -= atk
                if (to.hp > to.maxHp) to.hp = to.maxHp
                to.hp <= 0
            } else false
            to.parent.receive(ReceivedAttack(to, from, isFatal))
        }

    }

    private suspend inline fun <R> withLockedUnits(units: Set<GameUnit>, crossinline block: suspend () -> R): R? {
        val sortedUnits = units.toSortedSet(Comparator.comparing(GameUnit::id))
        val lockedMutexes = mutableListOf<Mutex>()
        try {
            for (unit in sortedUnits) {
                val mutex = unitMutexes[unit.id] ?: return null
                mutex.lock() // is ok if this mutex is already removed from map
                lockedMutexes.add(mutex)
            }
            return block()
        } finally {
            for (mutex in lockedMutexes.reversed()) mutex.unlock()
        }
    }

    private infix fun RGB.l1Norm(otherRgb: RGB): Double {
        val (r1, g1, b1) = this
        val (r2, g2, b2) = otherRgb
        return (abs(r1 - r2) + abs(g1 - g2) + abs(b1 - b2)).toDouble()
    }

    private infix fun RGB.similarityTo(baseColorId: BaseColorId): Double {
        val baseColorRGB = baseColorId.stats.rgb
        val (r2, g2, b2) = baseColorRGB
        return max(0.0, 1 - sqrt((this l1Norm baseColorRGB) / (1 + r2 + g2 + b2)))
    }

    private fun computeAttack(from: GameUnit, to: GameUnit): Int {
        val similarityCoefficient = from.gameColor similarityTo from.cachedBaseColorId
        return (attackFromTo[from.cachedBaseColorId to to.cachedBaseColorId]!! * similarityCoefficient).roundToInt()
    }

    suspend fun registerUnit(unit: GameUnit) {
        val mutex = Mutex()
        mutex.withLock {
            unitMutexes.put(unit.id, mutex)?.let { throw IllegalStateException("double unit register") }
        }
    }

    suspend fun unregisterUnit(unit: GameUnit) {
        val mutex = unitMutexes[unit.id]!!
        mutex.withLock {
            unitCachedBaseColorIds.remove(unit.id)
            unitMutexes.remove(unit.id) ?: throw IllegalStateException("attempt to unregister not registered unit")
        }
    }

    private fun resolveBaseColor(rgb: RGB): BaseColorId =
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
            val initialBaseColorId = unit.cachedBaseColorId
            for (updateEffect in initialBaseColorId.stats.updateEffects) {
                updateEffect.activate(
                    unit,
                    controlParams,
                    unsafeMethods
                )
                if (unit.cachedBaseColorId != initialBaseColorId) break
            }
        }
    }

    fun getBaseColorStats(unit: GameUnit): BaseColorStats = unit.cachedBaseColorId.stats

    private val BaseColorId.stats
        get() = baseColorStats[this] ?: throw IllegalStateException("no base color with such id")

    private val GameUnit.cachedBaseColorId
        get() = unitCachedBaseColorIds.getOrPut(this.id) { resolveBaseColor(this.gameColor) }

    private val teamGenerator = AtomicInteger(0)

    fun newTeamId() = teamGenerator.incrementAndGet()
}

