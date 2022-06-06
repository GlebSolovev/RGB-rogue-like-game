@file:Suppress("FunctionName")

package ru.hse.sd.rgb.utils

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.random.Random
import kotlin.reflect.KProperty

val ignore = Unit
val unreachable: Nothing get() = throw IllegalStateException("reached unreachable")

fun unreachable(info: Any): Nothing {
    System.err.println("unreachable! info: $info")
    unreachable
}

val notAllowed: Nothing get() = error("method not allowed")

operator fun <T, U, V> Map<Pair<T, U>, V>.get(t: T, u: U) = this[t to u]

operator fun <Y> AtomicReference<Y>.getValue(x: Any?, p: KProperty<*>): Y = this.get()
operator fun <Y> AtomicReference<Y>.setValue(x: Any?, p: KProperty<*>, value: Y) {
    this.set(value)
}

fun Random.nextChance(probability: Double): Boolean = this.nextDouble() < probability

fun <T> ConcurrentHashSet(): MutableSet<T> = Collections.newSetFromMap(ConcurrentHashMap())

fun <T> Collection<T>.randomElementOrNull(random: Random = Random) = this.shuffled(random).firstOrNull()
fun <T> Collection<T>.randomElement(random: Random = Random) = this.shuffled(random).first()

private const val EPS = 1e-8
infix fun Double.sameAs(d: Double) = abs(this - d) < EPS

val Int.d
    get() = toDouble()

infix fun Double.imul(v: Int) = (this * v).toInt()
