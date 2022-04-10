package ru.hse.sd.rgb.views

import ru.hse.sd.rgb.*
import ru.hse.sd.rgb.Ticker.Companion.Ticker
import java.awt.*
import java.awt.event.*
import java.awt.geom.Ellipse2D
import javax.swing.JFrame
import javax.swing.JPanel

class SwingView(
    private val offsetX: Int,
    private val offsetY: Int,
    private val frameWidth: Int,
    private val frameHeight: Int,
    private val tileSize: Int,
) : View() {

    private inner class SwingPlayingState : PlayingState() {
        override fun next(m: Message): ViewState = when (m) {
            is Ticker.Tick -> this.also { gamePanel.repaint() }
            is UserToggledInventory -> TODO()
            else -> this
        }
    }

    private inner class SwingPlayingInventoryState : PlayingInventoryState() {
        override fun next(m: Message): ViewState {
            TODO("Not yet implemented")
        }
    }

    private inner class SwingLoadingState : LoadingState() {
        override fun next(m: Message): ViewState {
            TODO("Not yet implemented")
        }
    }

    private val playingState = SwingPlayingState()
    private val playingInventoryState = SwingPlayingInventoryState()
    private val loadingState = SwingLoadingState()

    override var state: ViewState = playingState

    private inner class GamePanel : JPanel() {

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
            super.paintComponent(graphics)
            val g = graphics as Graphics2D
            g.color = Color.BLACK
            g.fillRect(0, 0, frameWidth, frameHeight)
            when (state) { // TODO: STATE IS NOT THREAD-SAFE
                is PlayingState -> drawWorld(g)
                is PlayingInventoryState -> drawInventory(g)
                is LoadingState -> drawLoading(g)
            }
        }

        private fun drawWorld(g: Graphics2D) {
            for ((_, viewUnits) in drawables) {
                for (viewUnit in viewUnits) {
                    val (shape, color) = viewUnit.getSwingAppearance()
                    g.color = color
                    g.fill(convertToSwingShape(shape, viewUnit.cell))
                }
            }
        }

        private fun drawInventory(g: Graphics2D) {
            TODO()
        }

        private fun drawLoading(g: Graphics2D) {
            TODO()
        }

    }

    private val gamePanel = GamePanel()
    private val ticker = Ticker(10) // TODO: magic constant

    fun initialize() {
        val window = JFrame("RGB") // TODO: magic constant (config?)
        window.defaultCloseOperation = JFrame.EXIT_ON_CLOSE // TODO: maybe save smth on exit?
        window.setSize(frameWidth, frameHeight)
        window.isVisible = true
        window.isFocusable = true
        window.add(gamePanel)

        gamePanel.isFocusable = true
        gamePanel.requestFocusInWindow()

        gamePanel.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                val message: Message = when (e.keyCode) {
                    KeyEvent.VK_W -> UserMoved(Direction.UP)
                    KeyEvent.VK_A -> UserMoved(Direction.LEFT)
                    KeyEvent.VK_S -> UserMoved(Direction.DOWN)
                    KeyEvent.VK_D -> UserMoved(Direction.RIGHT)
                    KeyEvent.VK_I -> UserToggledInventory()
                    else -> return
                }
                if (message is UserToggledInventory || state is PlayingInventoryState) {
                    inventoryListeners
                } else {
                    movementListeners
                }.forEach { it.receive(message) }
            }
        })

        window.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                ticker.stop()
            }
        })

        ticker.start()
    }

}

fun GameColor.toSwingColor() = Color(r, g, b)
