@file:Suppress("WildcardImport")

package ru.hse.sd.rgb.views.swing

import ru.hse.sd.rgb.controller
import ru.hse.sd.rgb.gamelogic.entities.HpGameUnit
import ru.hse.sd.rgb.gamelogic.items.InventoryViewSnapshot
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.utils.structures.Cell
import ru.hse.sd.rgb.views.DrawablesMap
import javax.swing.JPanel
import java.awt.*
import java.awt.font.TextLayout
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

private fun Graphics2D.enableFancyRendering() {
    this.setRenderingHints(
        mapOf(
            RenderingHints.KEY_RENDERING to RenderingHints.VALUE_RENDER_QUALITY,

            // only this option makes nice circles, but it also makes walls tiled
            RenderingHints.KEY_ANTIALIASING to RenderingHints.VALUE_ANTIALIAS_ON,

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
    return s.shape.convertToSwingShape(spX, spY, (tileSize * s.scale).roundToInt())
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

    @Suppress("MagicNumber")
    override fun paintComponent(graphics: Graphics) {
        val g = graphics as Graphics2D
        g.enableFancyRendering()
        g.color = bgColor
        g.fillRect(0, 0, width, height)
        g.stroke = BasicStroke(3.0f)
        for ((_, viewUnits) in drawables) {
            for (viewUnit in viewUnits) {
                g.color = viewUnit.rgb.toSwingColor()

                val (pxX, pxY) = convertCellToPixels(viewUnit.cell)
                val shape = convertToSwingShape(viewUnit.swingAppearance, pxX, pxY, tileSize)
                g.fill(shape)

                val outlineColor = viewUnit.swingAppearance.outlineRGB?.toSwingColor()
                if (outlineColor != null) {
                    g.color = outlineColor
                    g.draw(shape)
                }
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

    @Suppress("MagicNumber")
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

        val textMarginCoefficient = 0.05 // TODO: constant (or parameter)

        val selItem = invSnapshot.itemsGrid[selCell]
        if (selItem != null) {
            val itemsMaxX = invOffsetX + invGridW * itemSize
            val descMargin = ((width - itemsMaxX) * textMarginCoefficient).toInt()

            g.drawTextCustom(selItem.description, itemsMaxX + descMargin, invOffsetY)
        }

        val statsMargin = (invOffsetX * textMarginCoefficient).toInt()
        val hero = controller.hero
        val unitsHp = hero.units.map { (it as HpGameUnit).hp }
        val unitsRgb = hero.units.map { it.gameColor }
        g.drawTextCustom("Stats:   HP=$unitsHp   RGB=$unitsRgb", statsMargin, invOffsetY)
        // TODO: display info per each unit properly
    }
}

class LoadingPanel : JPanel() {
    @Suppress("MagicNumber")
    override fun paintComponent(graphics: Graphics) {
        val g = graphics as Graphics2D
        g.color = Color.BLACK
        g.fillRect(0, 0, width, height)
        g.color = Color.WHITE
        g.stroke = BasicStroke(5f)
        g.drawArc(width / 2, height / 2, 50, 50, ((System.currentTimeMillis() / 10) % 360).toInt(), 300)
        // TODO: spinning color square
    }
}

private const val CUSTOM_FONT_SIZE = 20
private val customFont = Font(Font.SANS_SERIF, Font.BOLD, CUSTOM_FONT_SIZE)

fun Graphics2D.drawTextCustom(text: String, atX: Int, atY: Int) {
    val textLayout = TextLayout(text, customFont, fontRenderContext)
    textLayout.draw(this, atX.toFloat(), atY.toFloat())
}
