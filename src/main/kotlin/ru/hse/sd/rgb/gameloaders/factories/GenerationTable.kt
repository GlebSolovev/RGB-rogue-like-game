package ru.hse.sd.rgb.gameloaders.factories

import kotlin.random.Random

data class TableEntry<T>(
    val weight: Int,
    val result: () -> T
)

class GenerationTable<T>(
    private val entries: List<TableEntry<T>>,
    private val random: Random = Random
) {

    private val weightSum: Int
    private val cdfList = mutableListOf<Int>()

    init {
        var sum = 0
        for (e in entries) {
            cdfList.add(sum)
            sum += e.weight
        }
        weightSum = sum
    }

    fun roll(): T { // NOTE: https://youtu.be/dQw4w9WgXcQ
        val value = random.nextInt(weightSum)
        val index = cdfList.binarySearch(value).let { if (it < 0) -it - 2 else it }
        return entries[index].result()
    }

    companion object {
        private class BuilderSimpleImpl<U> : GenerationTableBuilder<U> {

            private val outcomes = mutableListOf<TableEntry<U>>()
            private var random: Random? = null

            override fun useRandom(random: Random): GenerationTableBuilder<U> {
                this.random = random
                return this
            }
            override fun outcome(weight: Int, result: () -> U): GenerationTableBuilder<U> {
                require(weight > 0) { "weight must be positive" }
                outcomes.add(TableEntry(weight, result))
                return this
            }

            override fun build(): GenerationTable<U> {
                return GenerationTable(outcomes, random ?: Random)
            }
        }

        fun <U> builder(): GenerationTableBuilder<U> = BuilderSimpleImpl()
    }
}

interface GenerationTableBuilder<T> {
    fun useRandom(random: Random): GenerationTableBuilder<T>
    fun outcome(weight: Int, result: () -> T): GenerationTableBuilder<T>
    fun build(): GenerationTable<T>
}
