package ru.hse.sd.rgb.views.swing

import ru.hse.sd.rgb.gamelogic.engines.items.InventoryViewSnapshot
import ru.hse.sd.rgb.utils.Cell
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.views.DrawablesMap
import java.awt.*
import java.awt.geom.Ellipse2D
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JPanel

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

    private fun convertToSwingShape(s: SwingUnitShape, at: Cell): Shape {
        val (pxX, pxY) = convertCellToPixels(at)
        return when (s) {
            SwingUnitShape.SQUARE -> Rectangle(pxX, pxY, tileSize, tileSize)
            SwingUnitShape.CIRCLE -> Ellipse2D.Double(
                pxX.toDouble(),
                pxY.toDouble(),
                tileSize.toDouble(),
                tileSize.toDouble(),
            )
            SwingUnitShape.TRIANGLE -> Polygon(
                intArrayOf(pxX, pxX + tileSize / 2, pxX + tileSize),
                intArrayOf(pxY + tileSize, pxY, pxY + tileSize),
                3
            )
        }
    }

    override fun paintComponent(graphics: Graphics) {
        val g = graphics as Graphics2D
        g.color = bgColor
        g.fillRect(0, 0, width, height)
        for ((_, viewUnits) in drawables) {
            for (viewUnit in viewUnits) {
                val (shape) = viewUnit.swingAppearance
                g.color = viewUnit.rgb.toSwingColor()
                g.fill(convertToSwingShape(shape, viewUnit.cell))
            }
        }
    }

}

class GameInventoryPanel(
    gamePanel: GamePanel,
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
        this.add(gamePanel)

        val scale = invSnapshot.swingAppearance.scale
        val wGrid = invSnapshot.itemsGrid.w
        val hGrid = invSnapshot.itemsGrid.h

        val wPx = width * scale
        val hPx = height * scale

        val extraOffsetX = width * (1.0 - scale) / 2.0
        val extraOffsetY = height * (1.0 - scale) / 2.0

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
        g.color = Color(0, 0, 0, invSnapshot.swingAppearance.bgAlpha)
        g.fillRect(0, 0, width, height)
        g.color = Color.WHITE
        g.stroke = BasicStroke(3.0f)

        checkInventoryDimensions()

        for (gy in 0 until invGridH) {
            for (gx in 0 until invGridW) {
                val pxX = invOffsetX + gx * itemSize
                val pxY = invOffsetY + gy * itemSize
                g.drawRect(pxX, pxY, itemSize, itemSize)
                val item = invSnapshot.itemsGrid[Cell(gx, gy)]
                if (item != null) g.drawImage(item.getSwingAppearance(), pxX, pxY, itemSize, itemSize, null)
            }
        }
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