package com.keepstraight.desktop.alert

import com.keepstraight.desktop.presentation.DesktopMessageKey
import com.keepstraight.desktop.ui.i18n.DesktopMessageJvm
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
        val title = DesktopMessageJvm.text(DesktopMessageKey.ALERT_APP_NAME)
        val message = when (event) {
            DesktopAlertEvent.SLUMP_INITIAL -> DesktopMessageJvm.text(DesktopMessageKey.ALERT_SLOUCH_INITIAL)
            DesktopAlertEvent.SLUMP_REPEAT -> DesktopMessageJvm.text(DesktopMessageKey.ALERT_SLOUCH_REPEAT)
        }

        var notified = false
        if (notificationEnabled()) {
            val result = NativeDesktopNotifier.notify(title, message)
            notified = result.shown
            if (!result.shown) {
                System.err.println("KeepStraight: native notification failed (${result.detailKey})")
            } else if (result.limited && result.detailKey != null) {
                System.err.println("KeepStraight: native notification limited (${result.detailKey})")
            }
        }

        if (soundEnabled() || (notificationEnabled() && !notified)) {
            runCatching { Toolkit.getDefaultToolkit().beep() }
        }
    }

    fun dispose() = Unit
}
