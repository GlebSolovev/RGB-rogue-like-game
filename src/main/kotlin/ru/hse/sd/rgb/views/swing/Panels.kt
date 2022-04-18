package ru.hse.sd.rgb.views.swing

import ru.hse.sd.rgb.Cell
import ru.hse.sd.rgb.entities.common.GameEntity
import ru.hse.sd.rgb.views.GameEntityViewSnapshot
import java.awt.*
import java.awt.geom.Ellipse2D
import javax.swing.JPanel

class GamePanel(
    private val offsetX: Int,
    private val offsetY: Int,
    private val tileSize: Int,
    private val drawables: Map<GameEntity, GameEntityViewSnapshot>,
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