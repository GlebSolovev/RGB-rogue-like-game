package ru.hse.sd.rgb.utils.structures

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.hse.sd.rgb.utils.Cell

class Grid2D<T>(val w: Int, val h: Int, init: (Int, Int) -> T) : AbstractCollection<T>() {

//    constructor(w: Int, h: Int, init: () -> T) : this(w, h, { _, _ -> init() }) // no todo: report

    constructor(data: List<List<T>>) : this(data[0].size, data.size, { x, y -> data[y][x] }) {
        for (l in data) if (l.size != w) throw IllegalArgumentException("not rectangle data")
    }

    private val data = List(h) { y -> MutableList(w) { x -> init(x, y) } }

    override val size = w * h

    override fun iterator() = data.flatten().iterator()

    fun getRawView(): List<List<T>> = data

    // ------------- operators and extensions -------------

    operator fun get(cell: Cell) = data[cell.y][cell.x]
    operator fun get(x: Int, y: Int) = data[y][x]

    operator fun set(cell: Cell, value: T) {
        data[cell.y][cell.x] = value
    }

    operator fun set(x: Int, y: Int, value: T) {
        data[y][x] = value
    }

    // map

    fun <R> map(transform: (T) -> R): Grid2D<R> = Grid2D(w, h) { x, y -> transform(this[x, y]) }

    // withIndex

    data class CoordinatedValue<T>(val x: Int, val y: Int, val value: T)

    fun withCoords(): Iterable<CoordinatedValue<T>> =
        data.withIndex().map { (y, l) -> l.withIndex().map { (x, v) -> CoordinatedValue(x, y, v) } }.flatten()

    fun withIndex(): Nothing = throw UnsupportedOperationException()
}

object IntGrid2DSerializer : KSerializer<Grid2D<Int>> {
    override val descriptor = PrimitiveSerialDescriptor("Grid2D", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Grid2D<Int>) {
        val v = value.getRawView()
        val resultBuilder = StringBuilder("\n")
        for (list in v) {
            resultBuilder.append(list.joinToString(separator = " ")).append('\n')
        }
        encoder.encodeString(resultBuilder.toString())
    }

    override fun deserialize(decoder: Decoder): Grid2D<Int> {
        val s = decoder.decodeString()
        val lists = s.lineSequence()
            .filterNot { it == "" }
            .map { line ->
                line.split("\\s+".toRegex()).map { it.toInt() }
            }
            .toList()
        return Grid2D(lists)
    }
}
