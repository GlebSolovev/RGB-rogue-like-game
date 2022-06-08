@file:Suppress("FunctionName")

package ru.hse.sd.rgb.views.swing

import ru.hse.sd.rgb.utils.d
import ru.hse.sd.rgb.utils.imul
import ru.hse.sd.rgb.utils.structures.Direction
import ru.hse.sd.rgb.utils.structures.RGB
import java.awt.Polygon
import java.awt.Rectangle
import java.awt.Shape
import java.awt.geom.*
import kotlin.math.*

data class SwingUnitAppearance(
    val shape: SwingUnitShape,
    val outlineRGB: RGB?,
    val scale: Double = 1.0,
)

enum class SwingUnitShape {
    SQUARE {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            return Rectangle(spX, spY, scaledTileSize, scaledTileSize)
        }
    },
    CIRCLE {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            return Ellipse2D.Double(
                spX.toDouble(),
                spY.toDouble(),
                scaledTileSize.d,
                scaledTileSize.d
            )
        }
    },
    TRIANGLE_UP {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            return Polygon(
                intArrayOf(spX, spX + scaledTileSize / 2, spX + scaledTileSize),
                intArrayOf(spY + scaledTileSize, spY, spY + scaledTileSize),
                3
            )
        }
    },
    TRIANGLE_LEFT {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            return Polygon(
                intArrayOf(spX, spX + scaledTileSize, spX + scaledTileSize),
                intArrayOf(spY + scaledTileSize / 2, spY + scaledTileSize, spY),
                3
            )
        }
    },
    TRIANGLE_DOWN {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            return Polygon(
                intArrayOf(spX, spX + scaledTileSize / 2, spX + scaledTileSize),
                intArrayOf(spY, spY + scaledTileSize, spY),
                3
            )
        }
    },
    TRIANGLE_RIGHT {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            return Polygon(
                intArrayOf(spX, spX + scaledTileSize, spX),
                intArrayOf(spY, spY + scaledTileSize / 2, spY + scaledTileSize),
                3
            )
        }
    },
    CIRCLE_HALF_UP {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            return Arc2D.Double(spX.d, spY.d, scaledTileSize.d, scaledTileSize.d, 0.0, 180.0, Arc2D.PIE)
        }
    },
    CIRCLE_HALF_LEFT {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            return Arc2D.Double(spX.d, spY.d, scaledTileSize.d, scaledTileSize.d, 90.0, 180.0, Arc2D.PIE)
        }
    },
    CIRCLE_HALF_DOWN {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            return Arc2D.Double(spX.d, spY.d, scaledTileSize.d, scaledTileSize.d, 180.0, 180.0, Arc2D.PIE)
        }
    },
    CIRCLE_HALF_RIGHT {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            return Arc2D.Double(spX.d, spY.d, scaledTileSize.d, scaledTileSize.d, 270.0, 180.0, Arc2D.PIE)
        }
    },
    RECTANGLE_HORIZONTAL {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            return Rectangle(spX, spY + scaledTileSize / 3, scaledTileSize, scaledTileSize / 3)
        }
    },
    RECTANGLE_VERTICAL {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            return Rectangle(spX + scaledTileSize / 3, spY, scaledTileSize / 3, scaledTileSize)
        }
    },
    STAR_4 {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape =
            drawStar(spX, spY, scaledTileSize, 4, 4.0, 2.0)
    },
    STAR_8 {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape =
            drawStar(spX, spY, scaledTileSize, 8, 3.5, 2.0)
    },
    PLUS {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            val unit = scaledTileSize / 3
            // .  1  2  .
            // 11 12 3  4
            // 10 9  6  5
            // .  8  7  .
            val xs = listOf(
                1, 2, 2, 3,
                3, 2, 2, 1,
                1, 0, 0, 1
            ).map { spX + it * unit }.toIntArray()
            val ys = listOf(
                0, 0, 1, 1,
                2, 2, 3, 3,
                2, 2, 1, 1,
            ).map { spY + it * unit }.toIntArray()
            return Polygon(xs, ys, 12)
        }
    },
    SPINNING_SQUARE {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            val angleRad = ((System.currentTimeMillis() / 10) % 360 * 2 * PI / 360)
            val cos = cos(angleRad)
            val sin = sin(angleRad)
            val cX = spX + scaledTileSize / 2
            val cY = spY + scaledTileSize / 2
            val unit = scaledTileSize / 2
            return Polygon(
                intArrayOf(cX + (cos imul unit), cX - (sin imul unit), cX - (cos imul unit), cX + (sin imul unit)),
                intArrayOf(cY + (sin imul unit), cY + (cos imul unit), cY - (sin imul unit), cY - (cos imul unit)),
                4
            )
        }
    },
    SPIRAL {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            val scaleCoefficient = 0.8
            val unit = scaledTileSize / 2.0

            var curW = 2 * unit
            var curH = 2 * unit * scaleCoefficient
            fun offsetX() = (2 * unit - curW) / 2.0
            fun offsetY() = (2 * unit - curH) / 2.0

            fun arc(start: Double) =
                Arc2D.Double(spX + offsetX(), spY + offsetY(), curW, curH, start, 110.0, Arc2D.CHORD)

            var flag = true
            val arcs = (0..(5 * 90) step 90).map {
                val result = arc((it % 360).toDouble())
                if (flag) curW *= scaleCoefficient.pow(2) else curH *= scaleCoefficient.pow(2)
                flag = !flag
                result
            }

            val path = Path2D.Double()
            for (arc in arcs) {
                path.append(arc, false)
            }
            return path
        }
    },
    CROSS {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            // 1  16 .  14 13
            // 2  .  15 .  12
            // .  3  .  11 .
            // 4  .  7  .  10
            // 5  6  .  8  9
            val unit = scaledTileSize / 4
            val breathing = sin(System.currentTimeMillis() / 200.0) * 0.2
            val xs = listOf(
                0.0, 0.0, 1.0 + breathing, 0.0,
                0.0, 1.0 - breathing, 2.0, 3.0 + breathing,
                4.0, 4.0, 3.0 - breathing, 4.0,
                4.0, 3.0 + breathing, 2.0, 1.0 - breathing
            ).map { (spX + it * unit).toInt() }.toIntArray()
            val ys = listOf(
                0.0, 1.0 - breathing, 2.0, 3.0 + breathing,
                4.0, 4.0, 3.0 - breathing, 4.0,
                4.0, 3.0 + breathing, 2.0, 1.0 - breathing,
                0.0, 0.0, 1.0 + breathing, 0.0
            ).map { (spY + it * unit).toInt() }.toIntArray()
            return Polygon(xs, ys, 16)
        }
    },
    IDLE_PORTAL {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            val path = Path2D.Double()
            for (square in concentricSquares(spX, spY, scaledTileSize)) path.append(square, false)
            return path
        }
    },
    ACTIVE_PORTAL {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            val squares = concentricSquares(spX, spY, scaledTileSize)
            val centerX = spX + scaledTileSize / 2.0
            val centerY = spY + scaledTileSize / 2.0
            val rotatedSquares = squares.mapIndexed { i, s ->
                if (i == 0) {
                    s
                } else {
                    val angle = (System.currentTimeMillis() / 300.0 * (1.0 + i * 0.2))
                    val transform = AffineTransform().apply {
                        translate(centerX, centerY)
                        rotate(angle)
                        translate(-centerX, -centerY)
                    }
                    s.createTransformedArea(transform)
                }
            }
            return rotatedSquares.reduce { a1, a2 -> a1.apply { add(a2) } }
        }
    }
    ;

    protected fun hollowSquare(x: Int, y: Int, size: Int, thickness: Int = 1): Area {
        val outer = Rectangle(x, y, size, size)
        val inner = Rectangle(x + thickness, y + thickness, size - 2 * thickness, size - 2 * thickness)
        return Area(outer).apply { subtract(Area(inner)) }
    }

    // TODO: are not perfectly centered for reasons unknown
    protected fun concentricSquares(
        spX: Int,
        spY: Int,
        scaledTileSize: Int,
        count: Int = 5,
        scaleFactor: Double = 1.0 / sqrt(2.0)
    ): List<Area> {
        return List(count) {
            val currentScale = scaleFactor.pow(it)
            val currentSize = (scaledTileSize * currentScale).toInt()
            val x = spX + (scaledTileSize - currentSize) / 2
            val y = spY + (scaledTileSize - currentSize) / 2
            hollowSquare(x, y, currentSize)
        }
    }

    // sp = start position
    abstract fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape

    companion object {
        fun TRIANGLE(dir: Direction) = when (dir) {
            Direction.UP, Direction.NOPE -> TRIANGLE_UP
            Direction.LEFT -> TRIANGLE_LEFT
            Direction.DOWN -> TRIANGLE_DOWN
            Direction.RIGHT -> TRIANGLE_RIGHT
        }

        fun CIRCLE_HALF(dir: Direction) = when (dir) {
            Direction.UP, Direction.NOPE -> CIRCLE_HALF_UP
            Direction.LEFT -> CIRCLE_HALF_LEFT
            Direction.DOWN -> CIRCLE_HALF_DOWN
            Direction.RIGHT -> CIRCLE_HALF_RIGHT
        }

        fun RECTANGLE(dir: Direction) = when (dir) {
            Direction.UP, Direction.DOWN, Direction.NOPE -> RECTANGLE_VERTICAL
            Direction.LEFT, Direction.RIGHT -> RECTANGLE_HORIZONTAL
        }
    }
}

@Suppress("LongParameterList")
private fun drawStar(
    spX: Int,
    spY: Int,
    scaledTileSize: Int,
    numRays: Int,
    innerRadiusScale: Double,
    outerRadiusScale: Double,
    startAngleRad: Int = 0
): Shape {
    val centerX = spX + scaledTileSize / 2
    val centerY = spY + scaledTileSize / 2
    val innerRadius = scaledTileSize / innerRadiusScale
    val outerRadius = scaledTileSize / outerRadiusScale

    val path = Path2D.Double()
    val deltaAngleRad = Math.PI / numRays
    for (i in 0 until (numRays * 2)) {
        val angleRad = startAngleRad + i * deltaAngleRad
        val ca = cos(angleRad)
        val sa = sin(angleRad)
        val relX = ca * (if (i % 2 == 0) outerRadius else innerRadius)
        val relY = sa * (if (i % 2 == 0) outerRadius else innerRadius)

        if (i == 0) path.moveTo(centerX + relX, centerY + relY)
        else path.lineTo(centerX + relX, centerY + relY)
    }
    path.closePath()
    return path
}
