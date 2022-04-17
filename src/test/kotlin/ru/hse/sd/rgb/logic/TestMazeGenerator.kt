package ru.hse.sd.rgb.logic

import kotlin.random.Random
import kotlin.test.Test

class TestMazeGenerator {

    @Test
    fun print() {
        val w = 50
        val h = 50
        val grid = generateMaze(w, h, 5, 3, Random)

        for (j in 0 until h) {
            for (i in 0 until w) {
                print(if (grid[j][i]) '#' else '.')
            }
            println()
        }
    }

}