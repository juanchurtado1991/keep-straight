package com.keepstraight.desktop.ui.i18n

import androidx.compose.runtime.Composable
import com.keepstraight.desktop.generated.resources.Res
import com.keepstraight.desktop.generated.resources.msg_adb_connect_failed_body
import com.keepstraight.desktop.generated.resources.msg_adb_connect_failed_title
import com.keepstraight.desktop.generated.resources.msg_adb_connect_invalid_body
import com.keepstraight.desktop.generated.resources.msg_adb_connect_invalid_title
import com.keepstraight.desktop.generated.resources.msg_adb_connect_timeout_body
import com.keepstraight.desktop.generated.resources.msg_adb_connect_timeout_title
import com.keepstraight.desktop.generated.resources.msg_adb_install_failed_body
import com.keepstraight.desktop.generated.resources.msg_adb_install_failed_title
import com.keepstraight.desktop.generated.resources.msg_adb_install_failed_watch_body
import com.keepstraight.desktop.generated.resources.msg_adb_install_timeout_body
import com.keepstraight.desktop.generated.resources.msg_adb_install_timeout_title
import com.keepstraight.desktop.generated.resources.msg_adb_invalid_input_body
import com.keepstraight.desktop.generated.resources.msg_adb_invalid_input_title
import com.keepstraight.desktop.generated.resources.msg_adb_missing_body
import com.keepstraight.desktop.generated.resources.msg_adb_missing_title
import com.keepstraight.desktop.generated.resources.msg_adb_no_device_body
import com.keepstraight.desktop.generated.resources.msg_adb_no_device_title
import com.keepstraight.desktop.generated.resources.msg_adb_offline_body
import com.keepstraight.desktop.generated.resources.msg_adb_offline_title
import com.keepstraight.desktop.generated.resources.msg_adb_pair_failed_body
import com.keepstraight.desktop.generated.resources.msg_adb_pair_failed_title
import com.keepstraight.desktop.generated.resources.msg_adb_pair_timeout_body
import com.keepstraight.desktop.generated.resources.msg_adb_pair_timeout_title
import com.keepstraight.desktop.generated.resources.msg_adb_phone_apk_missing_body
import com.keepstraight.desktop.generated.resources.msg_adb_phone_apk_missing_title
import com.keepstraight.desktop.generated.resources.msg_adb_phone_not_ready_body
import com.keepstraight.desktop.generated.resources.msg_adb_phone_not_ready_title
import com.keepstraight.desktop.generated.resources.msg_adb_prepare_failed_body
import com.keepstraight.desktop.generated.resources.msg_adb_prepare_failed_title
import com.keepstraight.desktop.generated.resources.msg_adb_qr_browse_failed_body
import com.keepstraight.desktop.generated.resources.msg_adb_qr_browse_failed_title
import com.keepstraight.desktop.generated.resources.msg_adb_qr_connect_failed_body
import com.keepstraight.desktop.generated.resources.msg_adb_qr_connect_failed_title
import com.keepstraight.desktop.generated.resources.msg_adb_qr_connected
import com.keepstraight.desktop.generated.resources.msg_adb_qr_phone_found
import com.keepstraight.desktop.generated.resources.msg_adb_qr_waiting
import com.keepstraight.desktop.generated.resources.msg_adb_qr_waiting_phone_body
import com.keepstraight.desktop.generated.resources.msg_adb_qr_waiting_phone_title
import com.keepstraight.desktop.generated.resources.msg_adb_qr_watch_pair_body
import com.keepstraight.desktop.generated.resources.msg_adb_qr_watch_pair_title
import com.keepstraight.desktop.generated.resources.msg_adb_scan_timeout_body
import com.keepstraight.desktop.generated.resources.msg_adb_scan_timeout_title
import com.keepstraight.desktop.generated.resources.msg_adb_unauthorized_body
import com.keepstraight.desktop.generated.resources.msg_adb_unauthorized_title
import com.keepstraight.desktop.generated.resources.msg_adb_uninstall_retry_timeout_body
import com.keepstraight.desktop.generated.resources.msg_adb_uninstall_retry_timeout_title
import com.keepstraight.desktop.generated.resources.msg_adb_watch_not_ready_body
import com.keepstraight.desktop.generated.resources.msg_adb_watch_not_ready_title
import com.keepstraight.desktop.generated.resources.msg_adb_wear_apk_missing_body
import com.keepstraight.desktop.generated.resources.msg_adb_wear_apk_missing_title
import com.keepstraight.desktop.generated.resources.msg_alert_app_name
import com.keepstraight.desktop.generated.resources.msg_alert_slouch_initial
import com.keepstraight.desktop.generated.resources.msg_alert_slouch_repeat
import com.keepstraight.desktop.generated.resources.msg_bridge_cant_reach
import com.keepstraight.desktop.generated.resources.msg_bridge_checking
import com.keepstraight.desktop.generated.resources.msg_bridge_client_missing_token
import com.keepstraight.desktop.generated.resources.msg_bridge_client_not_paired
import com.keepstraight.desktop.generated.resources.msg_bridge_client_pair_failed
import com.keepstraight.desktop.generated.resources.msg_bridge_client_pair_rejected
import com.keepstraight.desktop.generated.resources.msg_bridge_client_unauthorized
import com.keepstraight.desktop.generated.resources.msg_bridge_getting_code
import com.keepstraight.desktop.generated.resources.msg_bridge_looks_good
import com.keepstraight.desktop.generated.resources.msg_bridge_missing_host_code
import com.keepstraight.desktop.generated.resources.msg_bridge_not_linked_setup
import com.keepstraight.desktop.generated.resources.msg_bridge_paired_protocol
import com.keepstraight.desktop.generated.resources.msg_bridge_paired_syncing
import com.keepstraight.desktop.generated.resources.msg_bridge_paired_with
import com.keepstraight.desktop.generated.resources.msg_bridge_pairing_failed
import com.keepstraight.desktop.generated.resources.msg_bridge_rejected
import com.keepstraight.desktop.generated.resources.msg_bridge_scan_qr
import com.keepstraight.desktop.generated.resources.msg_bridge_sync_trouble
import com.keepstraight.desktop.generated.resources.msg_bridge_unlinked
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
import com.keepstraight.desktop.generated.resources.msg_wizard_address_invalid
import com.keepstraight.desktop.generated.resources.msg_wizard_address_invalid_body
import com.keepstraight.desktop.generated.resources.msg_wizard_installed_phone
import com.keepstraight.desktop.generated.resources.msg_wizard_installed_watch
import com.keepstraight.desktop.generated.resources.msg_wizard_installing_phone
import com.keepstraight.desktop.generated.resources.msg_wizard_installing_watch
import com.keepstraight.desktop.generated.resources.msg_wizard_preparing
import com.keepstraight.desktop.generated.resources.msg_wizard_scan_qr_phone
import com.keepstraight.desktop.generated.resources.msg_wizard_stopped
import com.keepstraight.desktop.generated.resources.msg_wizard_stopped_new_code
import com.keepstraight.desktop.generated.resources.msg_wizard_unexpected_error
import com.keepstraight.desktop.generated.resources.msg_wizard_unexpected_error_body
import com.keepstraight.desktop.generated.resources.msg_wizard_watch_lookup
import com.keepstraight.desktop.generated.resources.msg_wizard_unexpected_error_body_watch
import com.keepstraight.desktop.generated.resources.msg_wizard_unexpected_error_body_phone
import com.keepstraight.desktop.generated.resources.msg_login_change_failed
import com.keepstraight.desktop.generated.resources.msg_adb_watch_debug_port_title
import com.keepstraight.desktop.generated.resources.msg_adb_watch_debug_port_body
import com.keepstraight.desktop.generated.resources.msg_adb_pairing_watch
import com.keepstraight.desktop.generated.resources.msg_adb_paired_finding_port
import com.keepstraight.desktop.generated.resources.msg_adb_connecting_watch
import com.keepstraight.desktop.generated.resources.msg_adb_connecting_install
import com.keepstraight.desktop.generated.resources.msg_wizard_watch_not_connected
import com.keepstraight.desktop.generated.resources.msg_wizard_watch_ready
import com.keepstraight.desktop.generated.resources.msg_wizard_watch_scan_failed
import com.keepstraight.desktop.generated.resources.msg_wizard_watch_scanning
import com.keepstraight.desktop.presentation.DesktopMessageKey
import com.keepstraight.desktop.presentation.UserMessage
import org.jetbrains.compose.resources.stringResource

object DesktopMessageResolver {
    @Composable
    fun text(key: DesktopMessageKey, vararg args: Any): String = when (key) {
        DesktopMessageKey.WIZARD_PREPARING -> stringResource(Res.string.msg_wizard_preparing)
        DesktopMessageKey.WIZARD_SCAN_QR_PHONE -> stringResource(Res.string.msg_wizard_scan_qr_phone)
        DesktopMessageKey.WIZARD_INSTALLING_PHONE -> stringResource(Res.string.msg_wizard_installing_phone)
        DesktopMessageKey.WIZARD_INSTALLED_PHONE -> stringResource(Res.string.msg_wizard_installed_phone)
        DesktopMessageKey.WIZARD_STOPPED_NEW_CODE -> stringResource(Res.string.msg_wizard_stopped_new_code)
        DesktopMessageKey.WIZARD_WATCH_SCANNING -> stringResource(Res.string.msg_wizard_watch_scanning)
        DesktopMessageKey.WIZARD_WATCH_READY -> stringResource(Res.string.msg_wizard_watch_ready)
        DesktopMessageKey.WIZARD_WATCH_NOT_CONNECTED -> stringResource(Res.string.msg_wizard_watch_not_connected)
        DesktopMessageKey.WIZARD_WATCH_SCAN_FAILED -> stringResource(Res.string.msg_wizard_watch_scan_failed)
        DesktopMessageKey.WIZARD_INSTALLING_WATCH -> stringResource(Res.string.msg_wizard_installing_watch)
        DesktopMessageKey.WIZARD_INSTALLED_WATCH -> stringResource(Res.string.msg_wizard_installed_watch)
        DesktopMessageKey.WIZARD_STOPPED -> stringResource(Res.string.msg_wizard_stopped)
        DesktopMessageKey.WIZARD_UNEXPECTED_ERROR -> stringResource(Res.string.msg_wizard_unexpected_error)
        DesktopMessageKey.WIZARD_UNEXPECTED_ERROR_BODY -> stringResource(Res.string.msg_wizard_unexpected_error_body)
        DesktopMessageKey.WIZARD_WATCH_LOOKUP -> stringResource(Res.string.msg_wizard_watch_lookup)
        DesktopMessageKey.WIZARD_UNEXPECTED_ERROR_BODY_WATCH -> stringResource(Res.string.msg_wizard_unexpected_error_body_watch)
        DesktopMessageKey.WIZARD_UNEXPECTED_ERROR_BODY_PHONE -> stringResource(Res.string.msg_wizard_unexpected_error_body_phone)
        DesktopMessageKey.ADB_WATCH_DEBUG_PORT_TITLE -> stringResource(Res.string.msg_adb_watch_debug_port_title)
        DesktopMessageKey.ADB_WATCH_DEBUG_PORT_BODY -> stringResource(Res.string.msg_adb_watch_debug_port_body)
        DesktopMessageKey.ADB_PAIRING_WATCH -> stringResource(Res.string.msg_adb_pairing_watch)
        DesktopMessageKey.ADB_PAIRED_FINDING_PORT -> stringResource(Res.string.msg_adb_paired_finding_port)
        DesktopMessageKey.ADB_CONNECTING_WATCH -> stringResource(Res.string.msg_adb_connecting_watch)
        DesktopMessageKey.ADB_CONNECTING_INSTALL -> stringResource(Res.string.msg_adb_connecting_install)
        DesktopMessageKey.WIZARD_ADDRESS_INVALID -> stringResource(Res.string.msg_wizard_address_invalid)
        DesktopMessageKey.WIZARD_ADDRESS_INVALID_BODY -> stringResource(Res.string.msg_wizard_address_invalid_body)
        DesktopMessageKey.BRIDGE_NOT_LINKED_SETUP -> stringResource(Res.string.msg_bridge_not_linked_setup)
        DesktopMessageKey.BRIDGE_CHECKING -> stringResource(Res.string.msg_bridge_checking)
        DesktopMessageKey.BRIDGE_CANT_REACH -> stringResource(Res.string.msg_bridge_cant_reach)
        DesktopMessageKey.BRIDGE_REJECTED -> stringResource(Res.string.msg_bridge_rejected)
        DesktopMessageKey.BRIDGE_SYNC_TROUBLE -> stringResource(Res.string.msg_bridge_sync_trouble)
        DesktopMessageKey.BRIDGE_LOOKS_GOOD -> stringResource(Res.string.msg_bridge_looks_good)
        DesktopMessageKey.BRIDGE_GETTING_CODE -> stringResource(Res.string.msg_bridge_getting_code)
        DesktopMessageKey.BRIDGE_SCAN_QR -> stringResource(Res.string.msg_bridge_scan_qr)
        DesktopMessageKey.BRIDGE_PAIRED_WITH -> stringResource(Res.string.msg_bridge_paired_with, args[0] as String)
        DesktopMessageKey.BRIDGE_UNLINKED -> stringResource(Res.string.msg_bridge_unlinked)
        DesktopMessageKey.BRIDGE_PAIRED_SYNCING -> stringResource(Res.string.msg_bridge_paired_syncing)
        DesktopMessageKey.BRIDGE_PAIRING_FAILED -> stringResource(Res.string.msg_bridge_pairing_failed)
        DesktopMessageKey.BRIDGE_MISSING_HOST_CODE -> stringResource(Res.string.msg_bridge_missing_host_code)
        DesktopMessageKey.BRIDGE_PAIRED_PROTOCOL -> stringResource(Res.string.msg_bridge_paired_protocol)
        DesktopMessageKey.TRAY_APP_NAME -> stringResource(Res.string.msg_tray_app_name)
        DesktopMessageKey.TRAY_OPEN -> stringResource(Res.string.msg_tray_open)
        DesktopMessageKey.TRAY_HIDE -> stringResource(Res.string.msg_tray_hide)
        DesktopMessageKey.TRAY_QUIT -> stringResource(Res.string.msg_tray_quit)
        DesktopMessageKey.TRAY_STILL_RUNNING -> stringResource(Res.string.msg_tray_still_running)
        DesktopMessageKey.TRAY_CLICK_ICON -> stringResource(Res.string.msg_tray_click_icon, args[0] as String)
        DesktopMessageKey.TRAY_WHERE_MENU_BAR -> stringResource(Res.string.msg_tray_where_menu_bar)
        DesktopMessageKey.TRAY_WHERE_NOTIFICATION -> stringResource(Res.string.msg_tray_where_notification)
        DesktopMessageKey.TRAY_WHERE_SYSTEM_TRAY -> stringResource(Res.string.msg_tray_where_system_tray)
        DesktopMessageKey.ALERT_APP_NAME -> stringResource(Res.string.msg_alert_app_name)
        DesktopMessageKey.ALERT_SLOUCH_INITIAL -> stringResource(Res.string.msg_alert_slouch_initial)
        DesktopMessageKey.ALERT_SLOUCH_REPEAT -> stringResource(Res.string.msg_alert_slouch_repeat)
        DesktopMessageKey.LOGIN_NO_LAUNCHER -> stringResource(Res.string.msg_login_no_launcher)
        DesktopMessageKey.LOGIN_ENABLED -> stringResource(Res.string.msg_login_enabled)
        DesktopMessageKey.LOGIN_REGISTER_FAILED -> stringResource(Res.string.msg_login_register_failed)
        DesktopMessageKey.LOGIN_DISABLED -> stringResource(Res.string.msg_login_disabled)
        DesktopMessageKey.LOGIN_OPEN_AT_UNAVAILABLE -> stringResource(Res.string.msg_login_open_at_unavailable)
        DesktopMessageKey.LOGIN_CHANGE_FAILED -> stringResource(Res.string.msg_login_change_failed)
        DesktopMessageKey.TEST_NOTIFICATION_TITLE -> stringResource(Res.string.msg_test_notification_title)
        DesktopMessageKey.TEST_NOTIFICATION_BODY -> stringResource(Res.string.msg_test_notification_body)
        DesktopMessageKey.TEST_NOTIFICATION_SENT -> stringResource(Res.string.msg_test_notification_sent)
        DesktopMessageKey.TEST_NOTIFICATION_LIMITED -> stringResource(Res.string.msg_test_notification_limited)
        DesktopMessageKey.TEST_NOTIFICATION_FAILED -> stringResource(Res.string.msg_test_notification_failed)
        DesktopMessageKey.NOTIFIER_MAC_BLOCKED -> stringResource(Res.string.msg_notifier_mac_blocked)
        DesktopMessageKey.NOTIFIER_MAC_HELPER_FAILED -> stringResource(Res.string.msg_notifier_mac_helper_failed)
        DesktopMessageKey.NOTIFIER_MAC_HELPER_MISSING -> stringResource(Res.string.msg_notifier_mac_helper_missing)
        DesktopMessageKey.NOTIFIER_TRAY_FALLBACK -> stringResource(Res.string.msg_notifier_tray_fallback)
        DesktopMessageKey.NOTIFIER_WINDOWS_FAILED -> stringResource(Res.string.msg_notifier_windows_failed)
        DesktopMessageKey.NOTIFIER_LINUX_NOTIFY_MISSING -> stringResource(Res.string.msg_notifier_linux_notify_missing)
        DesktopMessageKey.NOTIFIER_GENERIC_FAILURE -> stringResource(Res.string.msg_notifier_generic_failure)
        DesktopMessageKey.ADB_PREPARE_FAILED_TITLE -> stringResource(Res.string.msg_adb_prepare_failed_title)
        DesktopMessageKey.ADB_PREPARE_FAILED_BODY -> stringResource(Res.string.msg_adb_prepare_failed_body)
        DesktopMessageKey.ADB_MISSING_TITLE -> stringResource(Res.string.msg_adb_missing_title)
        DesktopMessageKey.ADB_MISSING_BODY -> stringResource(Res.string.msg_adb_missing_body)
        DesktopMessageKey.ADB_INVALID_INPUT_TITLE -> stringResource(Res.string.msg_adb_invalid_input_title)
        DesktopMessageKey.ADB_INVALID_INPUT_BODY -> stringResource(Res.string.msg_adb_invalid_input_body)
        DesktopMessageKey.ADB_PAIR_TIMEOUT_TITLE -> stringResource(Res.string.msg_adb_pair_timeout_title)
        DesktopMessageKey.ADB_PAIR_TIMEOUT_BODY -> stringResource(Res.string.msg_adb_pair_timeout_body)
        DesktopMessageKey.ADB_PAIR_FAILED_TITLE -> stringResource(Res.string.msg_adb_pair_failed_title)
        DesktopMessageKey.ADB_PAIR_FAILED_BODY -> stringResource(Res.string.msg_adb_pair_failed_body)
        DesktopMessageKey.ADB_CONNECT_INVALID_TITLE -> stringResource(Res.string.msg_adb_connect_invalid_title)
        DesktopMessageKey.ADB_CONNECT_INVALID_BODY -> stringResource(Res.string.msg_adb_connect_invalid_body)
        DesktopMessageKey.ADB_CONNECT_TIMEOUT_TITLE -> stringResource(Res.string.msg_adb_connect_timeout_title)
        DesktopMessageKey.ADB_CONNECT_TIMEOUT_BODY -> stringResource(Res.string.msg_adb_connect_timeout_body)
        DesktopMessageKey.ADB_CONNECT_FAILED_TITLE -> stringResource(Res.string.msg_adb_connect_failed_title)
        DesktopMessageKey.ADB_CONNECT_FAILED_BODY -> stringResource(Res.string.msg_adb_connect_failed_body)
        DesktopMessageKey.ADB_SCAN_TIMEOUT_TITLE -> stringResource(Res.string.msg_adb_scan_timeout_title)
        DesktopMessageKey.ADB_SCAN_TIMEOUT_BODY -> stringResource(Res.string.msg_adb_scan_timeout_body)
        DesktopMessageKey.ADB_NO_DEVICE_TITLE -> stringResource(Res.string.msg_adb_no_device_title)
        DesktopMessageKey.ADB_NO_DEVICE_BODY -> stringResource(Res.string.msg_adb_no_device_body)
        DesktopMessageKey.ADB_UNAUTHORIZED_TITLE -> stringResource(Res.string.msg_adb_unauthorized_title)
        DesktopMessageKey.ADB_UNAUTHORIZED_BODY -> stringResource(Res.string.msg_adb_unauthorized_body)
        DesktopMessageKey.ADB_OFFLINE_TITLE -> stringResource(Res.string.msg_adb_offline_title)
        DesktopMessageKey.ADB_OFFLINE_BODY -> stringResource(Res.string.msg_adb_offline_body)
        DesktopMessageKey.ADB_PHONE_NOT_READY_TITLE -> stringResource(Res.string.msg_adb_phone_not_ready_title)
        DesktopMessageKey.ADB_PHONE_NOT_READY_BODY -> stringResource(Res.string.msg_adb_phone_not_ready_body)
        DesktopMessageKey.ADB_WATCH_NOT_READY_TITLE -> stringResource(Res.string.msg_adb_watch_not_ready_title)
        DesktopMessageKey.ADB_WATCH_NOT_READY_BODY -> stringResource(Res.string.msg_adb_watch_not_ready_body)
        DesktopMessageKey.ADB_INSTALL_TIMEOUT_TITLE -> stringResource(Res.string.msg_adb_install_timeout_title)
        DesktopMessageKey.ADB_INSTALL_TIMEOUT_BODY -> stringResource(Res.string.msg_adb_install_timeout_body)
        DesktopMessageKey.ADB_INSTALL_FAILED_TITLE -> stringResource(Res.string.msg_adb_install_failed_title)
        DesktopMessageKey.ADB_INSTALL_FAILED_BODY -> stringResource(Res.string.msg_adb_install_failed_body)
        DesktopMessageKey.ADB_INSTALL_FAILED_WATCH_BODY -> stringResource(Res.string.msg_adb_install_failed_watch_body)
        DesktopMessageKey.ADB_UNINSTALL_RETRY_TIMEOUT_TITLE -> stringResource(Res.string.msg_adb_uninstall_retry_timeout_title)
        DesktopMessageKey.ADB_UNINSTALL_RETRY_TIMEOUT_BODY -> stringResource(Res.string.msg_adb_uninstall_retry_timeout_body)
        DesktopMessageKey.ADB_PHONE_APK_MISSING_TITLE -> stringResource(Res.string.msg_adb_phone_apk_missing_title)
        DesktopMessageKey.ADB_PHONE_APK_MISSING_BODY -> stringResource(Res.string.msg_adb_phone_apk_missing_body)
        DesktopMessageKey.ADB_WEAR_APK_MISSING_TITLE -> stringResource(Res.string.msg_adb_wear_apk_missing_title)
        DesktopMessageKey.ADB_WEAR_APK_MISSING_BODY -> stringResource(Res.string.msg_adb_wear_apk_missing_body)
        DesktopMessageKey.ADB_QR_WAITING -> stringResource(Res.string.msg_adb_qr_waiting)
        DesktopMessageKey.ADB_QR_PHONE_FOUND -> stringResource(Res.string.msg_adb_qr_phone_found)
        DesktopMessageKey.ADB_QR_CONNECTED -> stringResource(Res.string.msg_adb_qr_connected)
        DesktopMessageKey.ADB_QR_WAITING_PHONE_TITLE -> stringResource(Res.string.msg_adb_qr_waiting_phone_title)
        DesktopMessageKey.ADB_QR_WAITING_PHONE_BODY -> stringResource(Res.string.msg_adb_qr_waiting_phone_body)
        DesktopMessageKey.ADB_QR_CONNECT_FAILED_TITLE -> stringResource(Res.string.msg_adb_qr_connect_failed_title)
        DesktopMessageKey.ADB_QR_CONNECT_FAILED_BODY -> stringResource(Res.string.msg_adb_qr_connect_failed_body)
        DesktopMessageKey.ADB_QR_BROWSE_FAILED_TITLE -> stringResource(Res.string.msg_adb_qr_browse_failed_title)
        DesktopMessageKey.ADB_QR_BROWSE_FAILED_BODY -> stringResource(Res.string.msg_adb_qr_browse_failed_body)
        DesktopMessageKey.ADB_QR_WATCH_PAIR_TITLE -> stringResource(Res.string.msg_adb_qr_watch_pair_title)
        DesktopMessageKey.ADB_QR_WATCH_PAIR_BODY -> stringResource(Res.string.msg_adb_qr_watch_pair_body)
        DesktopMessageKey.PAIR_ASSIST_INVALID_QR -> stringResource(Res.string.msg_pair_assist_invalid_qr)
        DesktopMessageKey.PAIR_ASSIST_UPDATE_APP -> stringResource(Res.string.msg_pair_assist_update_app)
        DesktopMessageKey.BRIDGE_CLIENT_PAIR_FAILED -> stringResource(Res.string.msg_bridge_client_pair_failed, args[0] as String)
        DesktopMessageKey.BRIDGE_CLIENT_PAIR_REJECTED -> stringResource(Res.string.msg_bridge_client_pair_rejected)
        DesktopMessageKey.BRIDGE_CLIENT_MISSING_TOKEN -> stringResource(Res.string.msg_bridge_client_missing_token)
        DesktopMessageKey.BRIDGE_CLIENT_UNAUTHORIZED -> stringResource(Res.string.msg_bridge_client_unauthorized)
        DesktopMessageKey.BRIDGE_CLIENT_NOT_PAIRED -> stringResource(Res.string.msg_bridge_client_not_paired)
    }

    @Composable
    fun text(message: UserMessage?): String? = message?.let {
        it.override ?: text(it.key, *it.args.toTypedArray())
    }
}
