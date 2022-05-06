package ru.hse.sd.rgb.gamelogic

import ru.hse.sd.rgb.gameloaders.generators.generateMaze
import kotlin.random.Random

class TestMazeGenerator {

    fun print() {
        val w = 50
        val h = 50
        val grid = generateMaze(w, h, 5, 3, Random)

        for (y in 0 until h) {
            for (x in 0 until w) {
                print(if (grid[x, y]) '#' else '.')
            }
            println()
        }
    }

}