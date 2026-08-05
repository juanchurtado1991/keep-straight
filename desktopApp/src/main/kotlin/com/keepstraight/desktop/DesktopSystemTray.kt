package com.keepstraight.desktop

import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO
import java.awt.Color
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.RenderingHints
import java.awt.SystemTray

/**
 * Single AWT system-tray icon: menu + displayMessage notifications.
 */
class DesktopSystemTray {
    data class Labels(
        val tooltip: String,
        val open: String,
        val hide: String,
        val quit: String,
    )
    private val trayIcon = AtomicReference<java.awt.TrayIcon?>(null)
    private val onOpen = AtomicReference<() -> Unit>({})
    private val onHide = AtomicReference<() -> Unit>({})
    private val onQuit = AtomicReference<() -> Unit>({})

    fun setCallbacks(
        open: () -> Unit,
        hide: () -> Unit,
        quit: () -> Unit,
    ) {
        onOpen.set(open)
        onHide.set(hide)
        onQuit.set(quit)
    }

    fun install(labels: Labels): Boolean {
        if (!SystemTray.isSupported()) return false
        if (trayIcon.get() != null) return true
        return try {
            val icon = java.awt.TrayIcon(loadTrayImage(), labels.tooltip).apply {
                isImageAutoSize = true
                popupMenu = PopupMenu().apply {
                    add(MenuItem(labels.open).also {
                        it.addActionListener { onOpen.get().invoke() }
                    })
                    add(MenuItem(labels.hide).also {
                        it.addActionListener { onHide.get().invoke() }
                    })
                    addSeparator()
                    add(MenuItem(labels.quit).also {
                        it.addActionListener { onQuit.get().invoke() }
                    })
                }
                addActionListener { onOpen.get().invoke() }
            }
            SystemTray.getSystemTray().add(icon)
            trayIcon.set(icon)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun showNotification(title: String, body: String): Boolean {
        val icon = trayIcon.get() ?: return false
        return try {
            icon.displayMessage(title, body, java.awt.TrayIcon.MessageType.INFO)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun dispose() {
        val icon = trayIcon.getAndSet(null) ?: return
        runCatching { SystemTray.getSystemTray().remove(icon) }
    }

    private fun loadTrayImage(): BufferedImage {
        val stream = javaClass.classLoader.getResourceAsStream("icons/tray.png")
            ?: javaClass.classLoader.getResourceAsStream("icons/icon.png")
        if (stream != null) {
            stream.use { input ->
                ImageIO.read(input)?.let { return it }
            }
        }
        return fallbackImage()
    }

    /** Same geometry as [androidApp] adaptive icon if the PNG resource is missing. */
    private fun fallbackImage(): BufferedImage {
        val size = 64
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = Color(0x2E, 0x7D, 0x6F)
        g.fillRoundRect(0, 0, size, size, (size * 0.28f).toInt(), (size * 0.28f).toInt())
        g.color = Color.WHITE
        val stroke = (size * 0.08f).toInt().coerceAtLeast(2)
        g.stroke = java.awt.BasicStroke(stroke.toFloat())
        val pad = (size * 0.22f).toInt()
        g.drawOval(pad, pad, size - 2 * pad, size - 2 * pad)
        val cx = size / 2
        val cy = size / 2
        val arm = (size * 0.12f).toInt()
        g.fillRoundRect(cx - stroke / 2, cy - arm, stroke, arm, stroke, stroke)
        g.fillRoundRect(cx - arm, cy - stroke / 2, arm * 2, stroke, stroke, stroke)
        g.dispose()
        return img
    }
}
