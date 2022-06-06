package ru.hse.sd.rgb.utils.structures

import kotlin.math.abs
import kotlin.random.Random

object Paths2D {

    interface PathStrategy {
        fun next(from: Cell): Direction
    }

    fun straightLine(from: Cell, to: Cell): PathStrategy {
        val dx = to.x - from.x
        val dy = to.y - from.y

        if (dx == 0) return object : PathStrategy {
            private val dir = if (dy > 0) Direction.DOWN else Direction.UP
            override fun next(from: Cell) = dir
        }
        return object : PathStrategy {
            // (x-x1) / dx = (y-y1) / dy
            // x/dx - y/dy + (y1/dy - x1/dx) = 0
            private val a: Double = 1.0 / dx
            private val b: Double = -1.0 / dy
            private val c: Double = from.y.toDouble() / dy - from.x.toDouble() / dx
            private val prefDirs = listOf(
                if (dx > 0) Direction.RIGHT else Direction.LEFT,
                if (dy > 0) Direction.DOWN else Direction.UP
            )

            override fun next(from: Cell): Direction = prefDirs.sortedBy {
                val cell = from + it.toShift()
                abs(a * cell.x + b * cell.y + c) // divide by sqrt(a*a+b*b) to get distance
            }[0]
        }
    }

    fun randomWalk(random: Random = Random): PathStrategy = object : PathStrategy {
        override fun next(from: Cell) = Direction.random(random)
    }
}
