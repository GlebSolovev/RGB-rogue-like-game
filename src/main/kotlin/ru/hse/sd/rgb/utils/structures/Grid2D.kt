package ru.hse.sd.rgb.utils.structures

class Grid2D<T>(val w: Int, val h: Int, init: (Int, Int) -> T) : AbstractCollection<T>() {

    constructor(data: List<List<T>>) : this(data[0].size, data.size, { x, y -> data[y][x] }) {
        for (l in data) if (l.size != w) throw IllegalArgumentException("not rectangle data")
    }

    private val data = List(h) { y -> MutableList(w) { x -> init(x, y) } }

    override val size = w * h

    override fun iterator() = data.flatten().iterator()

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

    override fun toString(): String {
        val sb = StringBuilder()
        for (row in data) {
            for (v in row) sb.append(v.toString()).append(' ')
            sb.append('\n')
        }
        return sb.toString()
    }
}
