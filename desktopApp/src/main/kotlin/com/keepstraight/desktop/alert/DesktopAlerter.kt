package com.keepstraight.desktop.alert

import com.keepstraight.shared.domain.DesktopAlertEvent
import java.awt.Toolkit

/**
 * Desktop slump alerts: optional beep + native OS notification (background-capable).
 */
class DesktopAlerter(
    private val soundEnabled: () -> Boolean = { false },
    private val notificationEnabled: () -> Boolean = { true },
) {
    fun alert(event: DesktopAlertEvent) {
        val title = "KeepStraight"
        val message = when (event) {
            DesktopAlertEvent.SLUMP_INITIAL -> "You've been slouching — sit up straight."
            DesktopAlertEvent.SLUMP_REPEAT -> "Still slouching — sit up."
        }

        var notified = false
        if (notificationEnabled()) {
            val result = NativeDesktopNotifier.notify(title, message)
            notified = result.shown
            if (!result.shown) {
                System.err.println("KeepStraight: native notification failed (${result.detail})")
            } else if (result.limited && result.detail != null) {
                System.err.println("KeepStraight: native notification limited (${result.detail})")
            }
        }

        if (soundEnabled() || (notificationEnabled() && !notified)) {
            runCatching { Toolkit.getDefaultToolkit().beep() }
        }
    }

    fun dispose() = Unit
}
