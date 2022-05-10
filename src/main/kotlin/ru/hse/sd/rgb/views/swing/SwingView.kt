package ru.hse.sd.rgb.views.swing

import ru.hse.sd.rgb.gamelogic.engines.items.InventoryViewSnapshot
import ru.hse.sd.rgb.utils.Ticker.Companion.createTicker
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
            KeyEvent.VK_ENTER -> UserSelect()
            KeyEvent.VK_Q -> UserDrop()
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

    private data class SwingGameData(
        val gameGridW: Int,
        val gameGridH: Int,
        val gameBgColor: RGB,
    )

    data class SwingInventoryData(
        val invGridW: Int,
        val invGridH: Int,
    )

    private data class SwingPlayingData(
        val gameData: SwingGameData,
        val invData: SwingInventoryData
    )

    private var playingData: SwingPlayingData? = null

    // automaton start

    private inner class SwingPlayingState(
        gameDrawables: DrawablesMap,
    ) : PlayingState(gameDrawables) {

        init {
            drawables.putAll(gameDrawables)
            val (wGrid, hGrid, bgColor) = playingData!!.gameData

            val wPx = panel.width
            val hPx = panel.height
            // theoretically (with camera) these values should be calculated once for entire view
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
            is UserToggledInventory -> this.also {
                inventoryListeners.forEach { it.receive(m) }
            }
            is InventoryOpened -> SwingPlayingInventoryState(m.invSnapshot, drawables)
            is UserSelect -> this.also {
                ignore
            }
            is UserDrop -> this.also {
                ignore
            }
            is UserQuit -> this.also {
                quitListeners.forEach { it.receive(m) }
            }
            is EntityUpdated -> this.also {
                drawables[m.gameEntity] = m.newSnapshot
            }
            is EntityRemoved -> this.also {
                drawables.remove(m.gameEntity)!!
            }
            else -> unreachable(m)
        }

    }

    private inner class SwingPlayingInventoryState(
        inventoryViewSnapshot: InventoryViewSnapshot,
        private val drawables: DrawablesMap
    ) : PlayingInventoryState() {

        private val invPanel: GameInventoryPanel

        init {
            invPanel = GameInventoryPanel(
                panel.width,
                panel.height,
                inventoryViewSnapshot,
                playingData!!.invData
            )
//            switchPanel(invPanel) // TODO: do switch
            invPanel.isOpaque = false
            panel.add(invPanel)
            invPanel.setSize(panel.width, panel.height)
            invPanel.setLocation(0, 0)
        }

        // TODO: fight code duplicated in message handling
        override fun next(m: Message): ViewState = when (m) {
            is Tick -> this.also {
                panel.repaint()
            }
            is UserMoved -> this.also {
                movementListeners.forEach { it.receive(m) }
            }
            is UserToggledInventory -> this.also {
                inventoryListeners.forEach { it.receive(m) }
            }
            is InventoryClosed -> SwingPlayingState(drawables).also {
                panel.remove(invPanel)
            }
            is InventoryUpdated -> this.also {
                invPanel.invSnapshot = m.invSnapshot
            }
            is UserSelect -> this.also {
                inventoryListeners.forEach { it.receive(m) }
            }
            is UserDrop -> this.also {
                inventoryListeners.forEach { it.receive(m) }
            }
            is EntityUpdated -> this.also {
                drawables[m.gameEntity] = m.newSnapshot
            }
            is EntityRemoved -> this.also {
                drawables.remove(m.gameEntity)!!
            }
            is UserQuit -> this.also {
                quitListeners.forEach { it.receive(m) }
            }
            else -> unreachable(m)
        }
    }

    private inner class SwingLoadingState : LoadingState() {

        init {
            switchPanel(LoadingPanel())
        }

        override fun next(m: Message): ViewState = when (m) {
            is Tick -> this.also { panel.repaint() }
            is GameViewStarted -> {
                val (gameDesc, invDesc) = m.level
                playingData = SwingPlayingData(
                    SwingGameData(
                        gameDesc.gameGridW, gameDesc.gameGridH, gameDesc.gameBgColor
                    ),
                    SwingInventoryData(
                        invDesc.invGridW, invDesc.invGridH
                    )
                )

                SwingPlayingState(DrawablesMap())
            }
            is UserQuit -> this.also {
                quitListeners.forEach { it.receive(m) }
            }
            else -> this
        }
    }

    // automaton end

    override var state: AtomicReference<ViewState> = AtomicReference(SwingLoadingState())

    private val ticker: Ticker = createTicker(10) // TODO: magic constant

    override fun initialize() {
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
