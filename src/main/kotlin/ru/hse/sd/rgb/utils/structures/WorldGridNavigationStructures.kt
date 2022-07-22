package ru.hse.sd.rgb.utils.structures

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.random.Random

@Serializable(with = CellSerializer::class)
data class Cell(val x: Int, val y: Int)
@Serializable
data class GridShift(val dx: Int, val dy: Int)

operator fun Cell.plus(p: GridShift) = Cell(x + p.dx, y + p.dy)

operator fun Cell.minus(c: Cell) = GridShift(x - c.x, y - c.y)

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

    fun opposite(): Direction = when (this) {
        UP -> DOWN
        LEFT -> RIGHT
        DOWN -> UP
        RIGHT -> LEFT
        NOPE -> NOPE
    }

    val isVertical
        get() = this == UP || this == DOWN
    val isHorizontal
        get() = this == LEFT || this == RIGHT

    companion object {
        fun random(random: Random = Random) = listOf(UP, LEFT, DOWN, RIGHT).random(random)
    }
}

object CellSerializer : KSerializer<Cell> {
    override val descriptor = PrimitiveSerialDescriptor("Cell", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Cell) {
        val (x, y) = value
        encoder.encodeString("$x $y")
    }

    override fun deserialize(decoder: Decoder): Cell {
        val (x, y) = decoder.decodeString().split(" +".toRegex()).map { it.toInt() }
        return Cell(x, y)
    }
}
