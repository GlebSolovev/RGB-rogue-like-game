package ru.hse.sd.rgb.views.swing

import ru.hse.sd.rgb.gamelogic.engines.items.InventoryViewSnapshot
import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.views.DrawablesMap
import java.awt.*
import java.awt.geom.Ellipse2D
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JPanel
import kotlin.math.roundToInt

private fun Graphics2D.enableFancyRendering() {
    this.setRenderingHints(mapOf(
        RenderingHints.KEY_RENDERING to RenderingHints.VALUE_RENDER_QUALITY,
        RenderingHints.KEY_ANTIALIASING to RenderingHints.VALUE_ANTIALIAS_ON, // only this option makes nice circles, but it also makes walls tiled
        RenderingHints.KEY_DITHERING to RenderingHints.VALUE_DITHER_ENABLE,
        RenderingHints.KEY_INTERPOLATION to RenderingHints.VALUE_INTERPOLATION_BICUBIC,
    ))
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

    private fun convertToSwingShape(s: SwingUnitAppearance, at: Cell): Shape {
        val (pxX, pxY) = convertCellToPixels(at)
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
        }
    }

    override fun paintComponent(graphics: Graphics) {
        val g = graphics as Graphics2D
        g.enableFancyRendering()
        g.color = bgColor
        g.fillRect(0, 0, width, height)
        for ((_, viewUnits) in drawables) {
            for (viewUnit in viewUnits) {
                g.color = viewUnit.rgb.toSwingColor()
                g.fill(convertToSwingShape(viewUnit.swingAppearance, viewUnit.cell))
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
                if (item != null) g.drawImage(item.getSwingAppearance(), pxX, pxY, itemSize, itemSize, null)
            }
        }

        g.color = invSnapshot.swingAppearance.highlightColor
        val selCell = invSnapshot.selectedCell
        val pxX = invOffsetX + selCell.x * itemSize
        val pxY = invOffsetY + selCell.y * itemSize
        g.drawRect(pxX, pxY, itemSize, itemSize)
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