@file:Suppress("FunctionName")

package ru.hse.sd.rgb.views.swing

import ru.hse.sd.rgb.utils.d
import ru.hse.sd.rgb.utils.imul
import ru.hse.sd.rgb.utils.structures.Direction
import java.awt.Polygon
import java.awt.Rectangle
import java.awt.Shape
import java.awt.geom.Arc2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

data class SwingUnitAppearance(
    val shape: SwingUnitShape,
    val scale: Double = 1.0,
) // TODO: outline (for effects!)

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
    STAR_8 {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            val centerX = spX + scaledTileSize / 2
            val centerY = spY + scaledTileSize / 2
            val innerRadius = scaledTileSize / 3.5
            val outerRadius = scaledTileSize / 2.0
            val numRays = 8
            val startAngleRad = 0

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
    },
    PLUS {
        override fun convertToSwingShape(spX: Int, spY: Int, scaledTileSize: Int): Shape {
            val unit = scaledTileSize / 3
            //     1  2
            // 11  12 3  4
            // 10  9  6  5
            //     8  7
            val xs = intArrayOf(
                spX + unit, spX + 2 * unit, spX + 2 * unit, spX + 3 * unit,
                spX + 3 * unit, spX + 2 * unit, spX + 2 * unit, spX + unit,
                spX + unit, spX, spX, spX + unit
            )
            val ys = intArrayOf(
                spY, spY, spY + unit, spY + unit,
                spY + 2 * unit, spY + 2 * unit, spY + 3 * unit, spY + 3 * unit,
                spY + 2 * unit, spY + 2 * unit, spY + unit, spY + unit
            )
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
    }
    ;

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
