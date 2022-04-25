package ru.hse.sd.rgb.views.swing

import ru.hse.sd.rgb.utils.Ticker.Companion.Ticker
import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.*
import ru.hse.sd.rgb.views.*
import java.awt.*
import java.awt.event.*
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFrame
import javax.swing.JPanel

class SwingView : View() {

    private val window: JFrame = JFrame("RGB")
    private lateinit var panel: JPanel

    private fun handleKeyEvent(e: KeyEvent) {
        val message: Message = when (e.keyCode) {
            KeyEvent.VK_W, KeyEvent.VK_UP -> UserMoved(Direction.UP)
            KeyEvent.VK_A, KeyEvent.VK_LEFT -> UserMoved(Direction.LEFT)
            KeyEvent.VK_S, KeyEvent.VK_DOWN -> UserMoved(Direction.DOWN)
            KeyEvent.VK_D, KeyEvent.VK_RIGHT -> UserMoved(Direction.RIGHT)
            KeyEvent.VK_I -> UserToggledInventory()
            KeyEvent.VK_ESCAPE -> UserQuit()
            else -> return
        }
        this.receive(message)
    }

    private fun switchPanel(newPanel: JPanel) {
        if (this::panel.isInitialized) {
            val tmp = panel
            panel = newPanel
            window.add(panel)
            window.remove(tmp)
        } else {
            panel = newPanel
            window.add(panel)
        }

        panel.isFocusable = true
        panel.setBounds(0, 0, window.width, window.height)
        panel.isVisible = true
        panel.requestFocusInWindow()
        panel.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) = handleKeyEvent(e)
        })
        window.validate()
        window.repaint()
    }

    private inner class SwingPlayingState(
        drawables: MutableMap<GameEntity, GameEntityViewSnapshot>,
        wGrid: Int,
        hGrid: Int,
        bgColor: RGB,
    ) : PlayingState(drawables) {

        init {
            val wPx = panel.width
            val hPx = panel.height
            val (tileSize, offsetX, offsetY) = if (wPx / (wGrid + 2) > hPx / (hGrid + 2)) {
                val tileSize = hPx / (hGrid + 2)
                val offsetX = (wPx - wGrid * tileSize) / 2
                Triple(tileSize, offsetX, tileSize)
            } else {
                val tileSize = wPx / (wGrid + 2)
                val offsetY = (hPx - hGrid * tileSize) / 2
                Triple(tileSize, tileSize, offsetY)
            }
            switchPanel(GamePanel(offsetX, offsetY, tileSize, drawables, bgColor.toSwingColor()))
        }

        override fun next(m: Message): ViewState = when (m) {
            is Tick -> this.also {
                panel.repaint()
            }
            is UserMoved -> this.also {
                movementListeners.forEach { it.receive(m) }
            }
            is UserToggledInventory -> SwingPlayingInventoryState().also {
                inventoryListeners.forEach { it.receive(m) }
            }
            is UserQuit -> this.also {
                quitListeners.forEach { it.receive(m) }
            }
            is EntityMoved -> this.also {
                drawables[m.gameEntity] = m.nextSnapshot
            }
            else -> this
        }

    }

    private inner class SwingPlayingInventoryState : PlayingInventoryState() {
        override fun next(m: Message): ViewState {
            TODO("Not yet implemented")
        }
    }

    private inner class SwingLoadingState : LoadingState() {

        init {
            switchPanel(LoadingPanel())
        }

        override fun next(m: Message): ViewState = when (m) {
            is Tick -> this.also { panel.repaint() }
            is GameViewStarted -> {
                val (w, h, _, _, bgColor) = m.level
                SwingPlayingState(m.drawables, w, h, bgColor)
            }
            is UserQuit -> this.also {
                quitListeners.forEach { it.receive(m) }
            }
            else -> this
        }
    }

    override var state: AtomicReference<ViewState> = AtomicReference(SwingLoadingState())

    private val ticker: Ticker = Ticker(10) // TODO: magic constant

    fun initialize() {
        window.defaultCloseOperation = JFrame.EXIT_ON_CLOSE // TODO: maybe save something on exit?
        window.extendedState = JFrame.MAXIMIZED_BOTH
        window.isUndecorated = true
        window.isFocusable = true
        window.isVisible = true

        window.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                ticker.stop()
            }
        })

        ticker.start()
    }

}

fun RGB.toSwingColor() = Color(r, g, b)
