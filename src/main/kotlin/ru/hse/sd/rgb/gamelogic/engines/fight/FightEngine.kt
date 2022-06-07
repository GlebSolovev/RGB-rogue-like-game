package ru.hse.sd.rgb.gamelogic.engines.fight

import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.gamelogic.entities.GameUnitId
import ru.hse.sd.rgb.gamelogic.entities.HpGameUnit
import ru.hse.sd.rgb.utils.messaging.Ticker
import ru.hse.sd.rgb.utils.messaging.messages.ColorTick
import ru.hse.sd.rgb.utils.messaging.messages.HpChanged
import ru.hse.sd.rgb.utils.structures.Grid2D
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.utils.structures.RGBDelta
import ru.hse.sd.rgb.utils.structures.plus
import ru.hse.sd.rgb.utils.unreachable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
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
    val updatePeriodMillis: Long?,
    val updateEffects: List<BaseColorUpdateEffect>
)

@Suppress("TooManyFunctions")
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
    private val unitColorTickers = ConcurrentHashMap<GameUnitId, Ticker>() // TODO: not concurrent ?
    private val unsafeMethods: UnsafeMethodsImpl = UnsafeMethodsImpl()

    interface UnsafeMethods {
        fun unsafeChangeRGB(unit: GameUnit, newRgb: RGB)
        fun unsafeAttack(from: GameUnit, to: GameUnit)
        fun unsafeAttackDirectly(to: GameUnit, attack: Int)
    }

    // TODO: is it a good idea?
    private inner class UnsafeMethodsImpl : UnsafeMethods {
        override fun unsafeChangeRGB(unit: GameUnit, newRgb: RGB) {
            unit.gameColor = newRgb
            val newBaseColorId = resolveBaseColor(unit.gameColor)
            unitCachedBaseColorIds[unit.id] = newBaseColorId
            unsafeMethods.updateUnitTicker(unit, newBaseColorId.stats.updatePeriodMillis)
        }

        override fun unsafeAttack(from: GameUnit, to: GameUnit) {
            if (to !is HpGameUnit) return
            val atk = computeAttack(from, to)
            val sameTeam = from.parent.fightEntity.teamId == to.parent.fightEntity.teamId
            if (atk > 0 && sameTeam) return // don't attack teammates
            if (atk < 0 && !sameTeam) return // don't heal enemies
            unsafeAttackDirectly(to, atk)
        }

        fun updateUnitTicker(unit: GameUnit, updatePeriodMillis: Long?) {
            if (updatePeriodMillis == null) {
                unitColorTickers.remove(unit.id)?.stop()
            } else {
                val currentTicker = unitColorTickers[unit.id]
                if (currentTicker == null) {
                    val newTicker = Ticker(updatePeriodMillis, unit.parent, ColorTick(unit))
                    unitColorTickers.put(unit.id, newTicker)?.let { unreachable }
                    newTicker.start()
                } else {
                    currentTicker.periodMillis = updatePeriodMillis
                }
            }
        }

        override fun unsafeAttackDirectly(to: GameUnit, attack: Int) {
            if (to !is HpGameUnit) return
            val toStartHp = to.hp
            to.hp -= attack
            if (to.hp > to.maxHp) to.hp = to.maxHp
            if (toStartHp != to.hp) to.parent.receive(HpChanged(to, to.hp <= 0))
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
            unitMutexes.put(unit.id, mutex)?.let { error("double unit register") }
            unsafeMethods.updateUnitTicker(unit, getBaseColorStats(unit).updatePeriodMillis)
        }
    }

    suspend fun unregisterUnit(unit: GameUnit) {
        val mutex = unitMutexes[unit.id]!!
        mutex.withLock {
            unsafeMethods.updateUnitTicker(unit, null)
            unitCachedBaseColorIds.remove(unit.id) ?: unreachable
            unitMutexes.remove(unit.id) ?: error("attempt to unregister not registered unit")
        }
    }

    private fun resolveBaseColor(rgb: RGB): BaseColorId =
        baseColorStats.minWithOrNull(Comparator.comparing { it.value.rgb l1Norm rgb })!!.key

    suspend fun changeRGB(unit: GameUnit, newRgb: RGB) {
        withLockedUnits(setOf(unit)) {
            unsafeMethods.unsafeChangeRGB(unit, newRgb)
        }
    }

    suspend fun changeRGB(unit: GameUnit, delta: RGBDelta) {
        withLockedUnits(setOf(unit)) {
            unsafeMethods.unsafeChangeRGB(unit, unit.gameColor + delta)
        }
    }

    suspend fun attack(from: GameUnit, to: GameUnit) {
        withLockedUnits(setOf(from, to)) {
            unsafeMethods.unsafeAttack(from, to)
        }
    }

    // attack can be negative = heal
    suspend fun attackDirectly(to: GameUnit, attack: Int) {
        withLockedUnits(setOf(to)) {
            unsafeMethods.unsafeAttackDirectly(to, attack)
        }
    }

    suspend fun update(unit: GameUnit, controlParams: ControlParams) {
        if (!unit.parent.fightEntity.isUnitActive(unit)) return
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

    fun getBaseColorName(unit: GameUnit): String = getBaseColorStats(unit).name

    private fun getBaseColorStats(unit: GameUnit): BaseColorStats = unit.cachedBaseColorId.stats

    private val BaseColorId.stats
        get() = baseColorStats[this] ?: throw IllegalStateException("no base color with such id")

    private val GameUnit.cachedBaseColorId
        get() = unitCachedBaseColorIds.getOrPut(this.id) { resolveBaseColor(this.gameColor) }

    private val teamGenerator = AtomicInteger(0)

    fun newTeamId() = teamGenerator.incrementAndGet()
}
