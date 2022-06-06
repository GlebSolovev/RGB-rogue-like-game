package ru.hse.sd.rgb.gamelogic

import ru.hse.sd.rgb.gameloaders.generators.generateMaze
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.random.Random

class TestMazeGenerator {

    private fun print(): String {
        val w = 50
        val h = 50
        val grid = generateMaze(w, h, 5, 3, Random)

        val sb = StringBuilder()
        for (y in 0 until h) {
            for (x in 0 until w) {
                sb += if (grid[x, y]) '#' else '.'
            }
            sb += '\n'
        }
        return sb.toString()
    }

    @Test
    fun testPrint() {
        Assertions.assertDoesNotThrow { print() }
    }

    @Suppress("UnusedPrivateMember") // false positive by detekt
    private operator fun StringBuilder.plusAssign(c: Char) {
        this.append(c)
    }

}
