package ru.hse.sd.rgb.utils

import kotlinx.serialization.Serializable
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.random.Random
import kotlin.reflect.KProperty

@Serializable
data class RGB(val r: Int, val g: Int, val b: Int)

data class Cell(val x: Int, val y: Int)
data class GridShift(val dx: Int, val dy: Int)

operator fun Cell.plus(p: GridShift) = Cell(x + p.dx, y + p.dy)

enum class Direction {
    UP, LEFT, DOWN, RIGHT, NOPE;

    // (0, 0) is upper-left corner
    fun toShift(): GridShift = when (this) {
        UP -> GridShift(0, -1)
        LEFT -> GridShift(-1, 0)
        DOWN -> GridShift(0, 1)
        RIGHT -> GridShift(1, 0)
        NOPE -> GridShift(0, 0)
    }

    val isVertical
        get() = this == UP || this == DOWN
    val isHorizontal
        get() = this == LEFT || this == RIGHT

    companion object {
        fun random() = listOf(UP, LEFT, DOWN, RIGHT).random()
    }
}

val ignore = Unit
val unreachable: Nothing
    get() = throw RuntimeException("reached unreachable")

fun unreachable(info: Any): Nothing {
    System.err.println("unreachable! info: $info")
    unreachable
}

operator fun <T, U, V> Map<Pair<T, U>, V>.get(t: T, u: U) = this[t to u]

fun <T> Collection<T>.randomElementOrNull(random: Random = Random) = this.shuffled(random).firstOrNull()
fun <T> Collection<T>.randomElement(random: Random = Random) = this.shuffled(random).first()

fun GameEntity.randomCell(random: Random = Random) = this.units.randomElement(random).cell

operator fun <Y> AtomicReference<Y>.getValue(x: Any?, p: KProperty<*>): Y = this.get()
operator fun <Y> AtomicReference<Y>.setValue(x: Any?, p: KProperty<*>, value: Y) {
    this.set(value)
}

fun <T, R> Collection<T>.getAllSame(keyExtractor: (T) -> R): R? {
    val firstKey = keyExtractor(this.firstOrNull() ?: return null)
    return if (this.all { keyExtractor(it) == firstKey }) firstKey else null
}

fun generateRandomColor(random: Random = Random): RGB =
    List(3) { random.nextInt(256) }.let { (r, g, b) -> RGB(r, g, b) }

fun Random.nextChance(probability: Double): Boolean = this.nextDouble() < probability

infix fun Double.sameAs(d: Double) = abs(this - d) < 1e-8
