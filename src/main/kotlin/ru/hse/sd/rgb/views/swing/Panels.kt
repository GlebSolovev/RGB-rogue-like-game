package ru.hse.sd.rgb.views.swing

import ru.hse.sd.rgb.gamelogic.controller
import ru.hse.sd.rgb.gamelogic.entities.HpGameUnit
import ru.hse.sd.rgb.gamelogic.items.InventoryViewSnapshot
import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.views.DrawablesMap
import java.awt.*
import java.awt.font.TextLayout
import java.awt.geom.Arc2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JPanel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private fun Graphics2D.enableFancyRendering() {
    this.setRenderingHints(
        mapOf(
            RenderingHints.KEY_RENDERING to RenderingHints.VALUE_RENDER_QUALITY,
            RenderingHints.KEY_ANTIALIASING to RenderingHints.VALUE_ANTIALIAS_ON, // only this option makes nice circles, but it also makes walls tiled
            RenderingHints.KEY_DITHERING to RenderingHints.VALUE_DITHER_ENABLE,
            RenderingHints.KEY_INTERPOLATION to RenderingHints.VALUE_INTERPOLATION_BICUBIC,
        )
    )
}

private fun convertToSwingShape(
    s: SwingUnitAppearance,
    pxX: Int,
    pxY: Int,
    tileSize: Int
): Shape {
    val (spX, spY) = Cell(
        (pxX + (1.0 - s.scale) / 2 * tileSize).roundToInt(),
        (pxY + (1.0 - s.scale) / 2 * tileSize).roundToInt()
    )
    fun Int.scaled() = (this * s.scale).roundToInt()
    return when (s.shape) {
        SwingUnitShape.SQUARE -> Rectangle(spX, spY, tileSize.scaled(), tileSize.scaled())
        SwingUnitShape.CIRCLE -> Ellipse2D.Double(
            spX.toDouble(),
            spY.toDouble(),
            tileSize.toDouble(),
            tileSize.toDouble(),
        )
        SwingUnitShape.TRIANGLE_UP -> Polygon(
            intArrayOf(spX, spX + tileSize.scaled() / 2, spX + tileSize.scaled()),
            intArrayOf(spY + tileSize.scaled(), spY, spY + tileSize.scaled()),
            3
        )
        SwingUnitShape.TRIANGLE_RIGHT -> Polygon(
            intArrayOf(spX, spX + tileSize.scaled(), spX),
            intArrayOf(spY, spY + tileSize.scaled() / 2, spY + tileSize.scaled()),
            3
        )
        SwingUnitShape.TRIANGLE_DOWN -> Polygon(
            intArrayOf(spX, spX + tileSize.scaled() / 2, spX + tileSize.scaled()),
            intArrayOf(spY, spY + tileSize.scaled(), spY),
            3
        )
        SwingUnitShape.TRIANGLE_LEFT -> Polygon(
            intArrayOf(spX, spX + tileSize.scaled(), spX + tileSize.scaled()),
            intArrayOf(spY + tileSize.scaled() / 2, spY + tileSize.scaled(), spY),
            3
        )
        SwingUnitShape.CIRCLE_HALF_UP -> Arc2D.Double(
            spX.d, spY.d, tileSize.scaled().d, tileSize.scaled().d, 0.0, 180.0, Arc2D.PIE
        )
        SwingUnitShape.CIRCLE_HALF_LEFT -> Arc2D.Double(
            spX.d, spY.d, tileSize.scaled().d, tileSize.scaled().d, 90.0, 180.0, Arc2D.PIE
        )
        SwingUnitShape.CIRCLE_HALF_DOWN -> Arc2D.Double(
            spX.d, spY.d, tileSize.scaled().d, tileSize.scaled().d, 180.0, 180.0, Arc2D.PIE
        )
        SwingUnitShape.CIRCLE_HALF_RIGHT -> Arc2D.Double(
            spX.d, spY.d, tileSize.scaled().d, tileSize.scaled().d, 270.0, 180.0, Arc2D.PIE
        )
        SwingUnitShape.RECTANGLE_VERTICAL -> Rectangle(
            spX + tileSize.scaled() / 3,
            spY,
            tileSize.scaled() / 3,
            tileSize.scaled()
        )
        SwingUnitShape.RECTANGLE_HORIZONTAL -> Rectangle(
            spX,
            spY + tileSize.scaled() / 3,
            tileSize.scaled(),
            tileSize.scaled() / 3
        )
        SwingUnitShape.STAR_8 -> {
            val centerX = spX + tileSize.scaled() / 2
            val centerY = spY + tileSize.scaled() / 2
            val innerRadius = tileSize.scaled() / 3.5
            val outerRadius = tileSize.scaled() / 2.0
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
            path
        }
        SwingUnitShape.PLUS -> {
            val unit = tileSize.scaled() / 3
            //   1 2
            // B C 3 4
            // A 9 6 5
            //   8 7
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
        SwingUnitShape.SPINNING_SQUARE -> {
            val angleRad = ((System.currentTimeMillis() / 10) % 360 * 2 * PI / 360)
            val cos = cos(angleRad)
            val sin = sin(angleRad)
            val cX = spX + tileSize.scaled() / 2
            val cY = spY + tileSize.scaled() / 2
            val unit = tileSize.scaled() / 2
            return Polygon(
                intArrayOf(cX + (cos imul unit), cX - (sin imul unit), cX - (cos imul unit), cX + (sin imul unit)),
                intArrayOf(cY + (sin imul unit), cY + (cos imul unit), cY - (sin imul unit), cY - (cos imul unit)),
                4
            )
        }
        SwingUnitShape.SPIRAL -> {
            val scaleCoef = 0.6
            val unit = tileSize.scaled() / 2.0

            var curW = 2 * unit
            var curH = 2 * unit
            fun offsetX() = (2 * unit - curW) / 2.0
            fun offsetY() = (2 * unit - curH) / 2.0

            fun arc(start: Double) =
                Arc2D.Double(spX + offsetX(), spY + offsetY(), curW, curH, start, 100.0, Arc2D.CHORD)

            var flag = false
            val arcs = (0..450 step 90).map {
                if (flag) curW *= scaleCoef else curH *= scaleCoef
                flag = !flag

                arc((it % 360).toDouble())
            }

            val path = Path2D.Double()
            for (arc in arcs) {
                path.append(arc, false)
            }
            return path
        }
    }
}


open class GamePanel(
    private val offsetX: Int,
    private val offsetY: Int,
    private val tileSize: Int,
    private val drawables: DrawablesMap,
    private val bgColor: Color,
) : JPanel() {

    // from LU corner
    private fun convertCellToPixels(c: Cell): Pair<Int, Int> {
        return Pair(offsetX + c.x * tileSize, offsetY + c.y * tileSize)
    }

    override fun paintComponent(graphics: Graphics) {
        val g = graphics as Graphics2D
        g.enableFancyRendering()
        g.color = bgColor
        g.fillRect(0, 0, width, height)
        for ((_, viewUnits) in drawables) {
            for (viewUnit in viewUnits) {
                g.color = viewUnit.rgb.toSwingColor()
                val (pxX, pxY) = convertCellToPixels(viewUnit.cell)
                g.fill(convertToSwingShape(viewUnit.swingAppearance, pxX, pxY, tileSize))
            }
        }
    }

}

class GameInventoryPanel(
    w: Int,
    h: Int,
    invSnapshot: InventoryViewSnapshot,
    invData: SwingView.SwingInventoryData
) : JPanel() {

    var invSnapshot: InventoryViewSnapshot by AtomicReference(invSnapshot)

    private val itemSize: Int
    private val invOffsetX: Int
    private val invOffsetY: Int

    private val invGridW: Int = invData.invGridW
    private val invGridH: Int = invData.invGridH

    init {
        val scale = invSnapshot.swingAppearance.scale
        val wGrid = invSnapshot.itemsGrid.w
        val hGrid = invSnapshot.itemsGrid.h

        val wPx = w * scale
        val hPx = h * scale

        val extraOffsetX = w * (1.0 - scale) / 2.0
        val extraOffsetY = h * (1.0 - scale) / 2.0

        val (tileSize, offsetX, offsetY) = if (wPx / (wGrid + 2) > hPx / (hGrid + 2)) {
            val tileSize = hPx / (hGrid + 2)
            val offsetX = (wPx - wGrid * tileSize) / 2
            Triple(tileSize, offsetX + extraOffsetX, tileSize + extraOffsetY)
        } else {
            val tileSize = wPx / (wGrid + 2)
            val offsetY = (hPx - hGrid * tileSize) / 2
            Triple(tileSize, tileSize + extraOffsetX, offsetY + extraOffsetY)
        }

        this.itemSize = tileSize.toInt()
        this.invOffsetX = offsetX.toInt()
        this.invOffsetY = offsetY.toInt()
    }

    private fun checkInventoryDimensions() {
        if (invGridW != invSnapshot.itemsGrid.w || invGridH != invSnapshot.itemsGrid.h)
            throw IllegalArgumentException("inventory dimensions changed")
    }

    override fun paintComponent(graphics: Graphics) {
        val g = graphics as Graphics2D
        g.enableFancyRendering()

        g.color = Color(0, 0, 0, invSnapshot.swingAppearance.bgAlpha)
        g.fillRect(0, 0, width, height)

        checkInventoryDimensions()

        g.stroke = BasicStroke(3.0f)
        for (gy in 0 until invGridH) {
            for (gx in 0 until invGridW) {
                val pxX = invOffsetX + gx * itemSize
                val pxY = invOffsetY + gy * itemSize

                g.color = Color(0, 0, 0, invSnapshot.swingAppearance.itemBgAlpha)
                g.fillRect(pxX, pxY, itemSize, itemSize)

                g.color = invSnapshot.swingAppearance.gridColor
                g.drawRect(pxX, pxY, itemSize, itemSize)

                val item = invSnapshot.itemsGrid[Cell(gx, gy)]
                if (item != null) {
                    g.color = item.color.toSwingColor()
                    g.fill(convertToSwingShape(item.getSwingAppearance(), pxX, pxY, itemSize))
                }
            }
        }

        g.color = invSnapshot.swingAppearance.highlightColor
        val selCell = invSnapshot.selectedCell
        val pxX = invOffsetX + selCell.x * itemSize
        val pxY = invOffsetY + selCell.y * itemSize
        g.drawRect(pxX, pxY, itemSize, itemSize)

        val textMarginCoef = 0.05 // TODO: constant (or parameter)

        val selItem = invSnapshot.itemsGrid[selCell]
        if (selItem != null) {
            val itemsMaxX = invOffsetX + invGridW * itemSize
            val descMargin = ((width - itemsMaxX) * textMarginCoef).toInt()

            g.drawTextCustom(selItem.description, itemsMaxX + descMargin, invOffsetY)
        }

        val statsMargin = (invOffsetX * textMarginCoef).toInt()
        val hero = controller.hero
        val hps = hero.units.map { (it as HpGameUnit).hp }
        val rgbs = hero.units.map { it.gameColor }
        g.drawTextCustom("Stats:   HP=$hps   RGB=$rgbs", statsMargin, invOffsetY)
        // TODO: display info per each unit properly
    }

}

class LoadingPanel : JPanel() {
    override fun paintComponent(graphics: Graphics) {
        val g = graphics as Graphics2D
        g.color = Color.BLACK
        g.fillRect(0, 0, width, height)
        g.color = Color.WHITE
        g.stroke = BasicStroke(5f)
        g.drawArc(width / 2, height / 2, 50, 50, ((System.currentTimeMillis() / 10) % 360).toInt(), 300)
    }
}

private infix fun Double.imul(v: Int) = (this * v).toInt()

private val customFont = Font(Font.SANS_SERIF, Font.BOLD, 20)

fun Graphics2D.drawTextCustom(text: String, atX: Int, atY: Int) {
    val textLayout = TextLayout(text, customFont, fontRenderContext)
    textLayout.draw(this, atX.toFloat(), atY.toFloat())
}
