package com.keepstraight.desktop.ui.i18n

import com.keepstraight.desktop.generated.resources.Res
import com.keepstraight.desktop.generated.resources.msg_alert_app_name
import com.keepstraight.desktop.generated.resources.msg_alert_slouch_initial
import com.keepstraight.desktop.generated.resources.msg_alert_slouch_repeat
import com.keepstraight.desktop.generated.resources.msg_bridge_client_missing_token
import com.keepstraight.desktop.generated.resources.msg_bridge_client_not_paired
import com.keepstraight.desktop.generated.resources.msg_bridge_client_pair_failed
import com.keepstraight.desktop.generated.resources.msg_bridge_client_pair_rejected
import com.keepstraight.desktop.generated.resources.msg_bridge_client_unauthorized
import com.keepstraight.desktop.generated.resources.msg_bridge_pairing_failed
import com.keepstraight.desktop.generated.resources.msg_bridge_paired_protocol
import com.keepstraight.desktop.generated.resources.msg_login_change_failed
import com.keepstraight.desktop.generated.resources.msg_login_disabled
import com.keepstraight.desktop.generated.resources.msg_login_enabled
import com.keepstraight.desktop.generated.resources.msg_login_no_launcher
import com.keepstraight.desktop.generated.resources.msg_login_open_at_unavailable
import com.keepstraight.desktop.generated.resources.msg_login_register_failed
import com.keepstraight.desktop.generated.resources.msg_pair_assist_invalid_qr
import com.keepstraight.desktop.generated.resources.msg_pair_assist_update_app
import com.keepstraight.desktop.generated.resources.msg_notifier_generic_failure
import com.keepstraight.desktop.generated.resources.msg_notifier_linux_notify_missing
import com.keepstraight.desktop.generated.resources.msg_notifier_mac_blocked
import com.keepstraight.desktop.generated.resources.msg_notifier_mac_helper_failed
import com.keepstraight.desktop.generated.resources.msg_notifier_mac_helper_missing
import com.keepstraight.desktop.generated.resources.msg_notifier_tray_fallback
import com.keepstraight.desktop.generated.resources.msg_notifier_windows_failed
import com.keepstraight.desktop.generated.resources.msg_test_notification_body
import com.keepstraight.desktop.generated.resources.msg_test_notification_failed
import com.keepstraight.desktop.generated.resources.msg_test_notification_limited
import com.keepstraight.desktop.generated.resources.msg_test_notification_sent
import com.keepstraight.desktop.generated.resources.msg_test_notification_title
import com.keepstraight.desktop.generated.resources.msg_tray_app_name
import com.keepstraight.desktop.generated.resources.msg_tray_click_icon
import com.keepstraight.desktop.generated.resources.msg_tray_hide
import com.keepstraight.desktop.generated.resources.msg_tray_open
import com.keepstraight.desktop.generated.resources.msg_tray_quit
import com.keepstraight.desktop.generated.resources.msg_tray_still_running
import com.keepstraight.desktop.generated.resources.msg_tray_where_menu_bar
import com.keepstraight.desktop.generated.resources.msg_tray_where_notification
import com.keepstraight.desktop.generated.resources.msg_tray_where_system_tray
import com.keepstraight.desktop.presentation.DesktopMessageKey
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

/** Resolves desktop copy outside @Composable (tray, alerts, LAN protocol). */
object DesktopMessageJvm {
    fun text(key: DesktopMessageKey, vararg args: Any): String = runBlocking {
        when (key) {
            DesktopMessageKey.TRAY_APP_NAME -> getString(Res.string.msg_tray_app_name)
            DesktopMessageKey.TRAY_OPEN -> getString(Res.string.msg_tray_open)
            DesktopMessageKey.TRAY_HIDE -> getString(Res.string.msg_tray_hide)
            DesktopMessageKey.TRAY_QUIT -> getString(Res.string.msg_tray_quit)
            DesktopMessageKey.TRAY_STILL_RUNNING -> getString(Res.string.msg_tray_still_running)
            DesktopMessageKey.TRAY_CLICK_ICON -> getString(Res.string.msg_tray_click_icon, args[0] as String)
            DesktopMessageKey.TRAY_WHERE_MENU_BAR -> getString(Res.string.msg_tray_where_menu_bar)
            DesktopMessageKey.TRAY_WHERE_NOTIFICATION -> getString(Res.string.msg_tray_where_notification)
            DesktopMessageKey.TRAY_WHERE_SYSTEM_TRAY -> getString(Res.string.msg_tray_where_system_tray)
            DesktopMessageKey.ALERT_APP_NAME -> getString(Res.string.msg_alert_app_name)
            DesktopMessageKey.ALERT_SLOUCH_INITIAL -> getString(Res.string.msg_alert_slouch_initial)
            DesktopMessageKey.ALERT_SLOUCH_REPEAT -> getString(Res.string.msg_alert_slouch_repeat)
            DesktopMessageKey.LOGIN_NO_LAUNCHER -> getString(Res.string.msg_login_no_launcher)
            DesktopMessageKey.LOGIN_ENABLED -> getString(Res.string.msg_login_enabled)
            DesktopMessageKey.LOGIN_REGISTER_FAILED -> getString(Res.string.msg_login_register_failed)
            DesktopMessageKey.LOGIN_DISABLED -> getString(Res.string.msg_login_disabled)
            DesktopMessageKey.LOGIN_OPEN_AT_UNAVAILABLE -> getString(Res.string.msg_login_open_at_unavailable)
            DesktopMessageKey.LOGIN_CHANGE_FAILED -> getString(Res.string.msg_login_change_failed)
            DesktopMessageKey.TEST_NOTIFICATION_TITLE -> getString(Res.string.msg_test_notification_title)
            DesktopMessageKey.TEST_NOTIFICATION_BODY -> getString(Res.string.msg_test_notification_body)
            DesktopMessageKey.TEST_NOTIFICATION_SENT -> getString(Res.string.msg_test_notification_sent)
            DesktopMessageKey.TEST_NOTIFICATION_LIMITED -> getString(Res.string.msg_test_notification_limited)
            DesktopMessageKey.TEST_NOTIFICATION_FAILED -> getString(Res.string.msg_test_notification_failed)
            DesktopMessageKey.NOTIFIER_MAC_BLOCKED -> getString(Res.string.msg_notifier_mac_blocked)
            DesktopMessageKey.NOTIFIER_MAC_HELPER_FAILED -> getString(Res.string.msg_notifier_mac_helper_failed)
            DesktopMessageKey.NOTIFIER_MAC_HELPER_MISSING -> getString(Res.string.msg_notifier_mac_helper_missing)
            DesktopMessageKey.NOTIFIER_TRAY_FALLBACK -> getString(Res.string.msg_notifier_tray_fallback)
            DesktopMessageKey.NOTIFIER_WINDOWS_FAILED -> getString(Res.string.msg_notifier_windows_failed)
            DesktopMessageKey.NOTIFIER_LINUX_NOTIFY_MISSING -> getString(Res.string.msg_notifier_linux_notify_missing)
            DesktopMessageKey.NOTIFIER_GENERIC_FAILURE -> getString(Res.string.msg_notifier_generic_failure)
            DesktopMessageKey.PAIR_ASSIST_INVALID_QR -> getString(Res.string.msg_pair_assist_invalid_qr)
            DesktopMessageKey.PAIR_ASSIST_UPDATE_APP -> getString(Res.string.msg_pair_assist_update_app)
            DesktopMessageKey.BRIDGE_PAIRED_PROTOCOL -> getString(Res.string.msg_bridge_paired_protocol)
            DesktopMessageKey.BRIDGE_PAIRING_FAILED -> getString(Res.string.msg_bridge_pairing_failed)
            DesktopMessageKey.BRIDGE_CLIENT_PAIR_FAILED -> getString(
                Res.string.msg_bridge_client_pair_failed,
                args[0] as String,
            )
            DesktopMessageKey.BRIDGE_CLIENT_PAIR_REJECTED -> getString(Res.string.msg_bridge_client_pair_rejected)
            DesktopMessageKey.BRIDGE_CLIENT_MISSING_TOKEN -> getString(Res.string.msg_bridge_client_missing_token)
            DesktopMessageKey.BRIDGE_CLIENT_UNAUTHORIZED -> getString(Res.string.msg_bridge_client_unauthorized)
            DesktopMessageKey.BRIDGE_CLIENT_NOT_PAIRED -> getString(Res.string.msg_bridge_client_not_paired)
            else -> error("DesktopMessageJvm does not resolve $key")
        }
    }
}
