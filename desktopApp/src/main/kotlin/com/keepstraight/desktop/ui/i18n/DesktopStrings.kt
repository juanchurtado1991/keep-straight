package com.keepstraight.desktop.ui.i18n

import androidx.compose.runtime.Composable
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.sharedui.i18n.SharedStrings
import com.keepstraight.desktop.generated.resources.Res
import com.keepstraight.desktop.generated.resources.action_accept
import com.keepstraight.desktop.generated.resources.action_back
import com.keepstraight.desktop.generated.resources.action_calibrate
import com.keepstraight.desktop.generated.resources.action_cancel
import com.keepstraight.desktop.generated.resources.action_cancel_qr
import com.keepstraight.desktop.generated.resources.action_capture_erect_pose
import com.keepstraight.desktop.generated.resources.action_capture_slumped_pose
import com.keepstraight.desktop.generated.resources.action_checking
import com.keepstraight.desktop.generated.resources.action_connect_install
import com.keepstraight.desktop.generated.resources.action_continue
import com.keepstraight.desktop.generated.resources.action_close
import com.keepstraight.desktop.generated.resources.action_done
import com.keepstraight.desktop.generated.resources.action_done_linking
import com.keepstraight.desktop.generated.resources.action_enter_address_manually
import com.keepstraight.desktop.generated.resources.action_fix_phone_link
import com.keepstraight.desktop.generated.resources.action_hide_to_tray
import com.keepstraight.desktop.generated.resources.action_hide_window_now
import com.keepstraight.desktop.generated.resources.action_manage
import com.keepstraight.desktop.generated.resources.action_new_qr
import com.keepstraight.desktop.generated.resources.action_quit
import com.keepstraight.desktop.generated.resources.action_quit_app
import com.keepstraight.desktop.generated.resources.action_recalibrate_both
import com.keepstraight.desktop.generated.resources.action_reconnect
import com.keepstraight.desktop.generated.resources.action_redo_erect
import com.keepstraight.desktop.generated.resources.action_refresh
import com.keepstraight.desktop.generated.resources.action_refresh_cameras
import com.keepstraight.desktop.generated.resources.action_retry_camera
import com.keepstraight.desktop.generated.resources.action_retry_slumped_pose
import com.keepstraight.desktop.generated.resources.action_scan_new_qr
import com.keepstraight.desktop.generated.resources.action_search_again
import com.keepstraight.desktop.generated.resources.action_send_test_notification
import com.keepstraight.desktop.generated.resources.action_setup_again
import com.keepstraight.desktop.generated.resources.action_setup_phone_watch
import com.keepstraight.desktop.generated.resources.action_show_new_code
import com.keepstraight.desktop.generated.resources.action_show_qr_link_phone
import com.keepstraight.desktop.generated.resources.action_skip_for_now
import com.keepstraight.desktop.generated.resources.action_skip_phone_watch
import com.keepstraight.desktop.generated.resources.action_start
import com.keepstraight.desktop.generated.resources.action_stop
import com.keepstraight.desktop.generated.resources.action_stop_waiting
import com.keepstraight.desktop.generated.resources.action_try_again
import com.keepstraight.desktop.generated.resources.action_unlink_phone
import com.keepstraight.desktop.generated.resources.app_name
import com.keepstraight.desktop.generated.resources.app_tagline
import com.keepstraight.desktop.generated.resources.bridge_connected
import com.keepstraight.desktop.generated.resources.bridge_connected_to
import com.keepstraight.desktop.generated.resources.bridge_degraded_body
import com.keepstraight.desktop.generated.resources.bridge_degraded_title
import com.keepstraight.desktop.generated.resources.bridge_failed_body
import com.keepstraight.desktop.generated.resources.bridge_failed_title
import com.keepstraight.desktop.generated.resources.bridge_linked_title
import com.keepstraight.desktop.generated.resources.bridge_not_linked_body
import com.keepstraight.desktop.generated.resources.bridge_not_linked_title
import com.keepstraight.desktop.generated.resources.calibration_body_after_erect
import com.keepstraight.desktop.generated.resources.calibration_body_capture_erect
import com.keepstraight.desktop.generated.resources.calibration_body_capture_slump
import com.keepstraight.desktop.generated.resources.calibration_body_default
import com.keepstraight.desktop.generated.resources.calibration_complete
import com.keepstraight.desktop.generated.resources.calibration_hold_still
import com.keepstraight.desktop.generated.resources.calibration_issue_camera
import com.keepstraight.desktop.generated.resources.calibration_issue_model_missing
import com.keepstraight.desktop.generated.resources.calibration_issue_needs_erect
import com.keepstraight.desktop.generated.resources.calibration_issue_no_pose
import com.keepstraight.desktop.generated.resources.calibration_issue_poses_similar
import com.keepstraight.desktop.generated.resources.calibration_phase_hold_good
import com.keepstraight.desktop.generated.resources.calibration_phase_hold_slouch
import com.keepstraight.desktop.generated.resources.calibration_preview_cd
import com.keepstraight.desktop.generated.resources.calibration_starting_camera
import com.keepstraight.desktop.generated.resources.calibration_step1_erect
import com.keepstraight.desktop.generated.resources.calibration_step2_slumped
import com.keepstraight.desktop.generated.resources.calibration_title
import com.keepstraight.desktop.generated.resources.consent_camera_body
import com.keepstraight.desktop.generated.resources.consent_offline_body
import com.keepstraight.desktop.generated.resources.home_presence_slump
import com.keepstraight.desktop.generated.resources.home_privacy_note
import com.keepstraight.desktop.generated.resources.home_slump_score
import com.keepstraight.desktop.generated.resources.settings
import com.keepstraight.desktop.generated.resources.settings_alert_timing
import com.keepstraight.desktop.generated.resources.settings_bridge_degraded
import com.keepstraight.desktop.generated.resources.settings_bridge_failed
import com.keepstraight.desktop.generated.resources.settings_bridge_linked
import com.keepstraight.desktop.generated.resources.settings_bridge_not_linked
import com.keepstraight.desktop.generated.resources.settings_low_power_label
import com.keepstraight.desktop.generated.resources.settings_no_camera
import com.keepstraight.desktop.generated.resources.settings_notification_label
import com.keepstraight.desktop.generated.resources.settings_notification_subtitle_supported
import com.keepstraight.desktop.generated.resources.settings_notification_subtitle_unsupported
import com.keepstraight.desktop.generated.resources.settings_open_at_login_label
import com.keepstraight.desktop.generated.resources.settings_open_at_login_subtitle
import com.keepstraight.desktop.generated.resources.settings_open_at_login_unavailable
import com.keepstraight.desktop.generated.resources.settings_phone_owns_timers
import com.keepstraight.desktop.generated.resources.settings_qr_link_cd
import com.keepstraight.desktop.generated.resources.settings_section_alerts
import com.keepstraight.desktop.generated.resources.settings_section_camera
import com.keepstraight.desktop.generated.resources.settings_section_general
import com.keepstraight.desktop.generated.resources.settings_section_phone_watch
import com.keepstraight.desktop.generated.resources.settings_section_sensitivity
import com.keepstraight.desktop.generated.resources.settings_section_sensitivity_from_phone
import com.keepstraight.desktop.generated.resources.settings_camera_default_name
import com.keepstraight.desktop.generated.resources.sensitivity_relaxed
import com.keepstraight.desktop.generated.resources.sensitivity_normal
import com.keepstraight.desktop.generated.resources.sensitivity_strict
import com.keepstraight.desktop.generated.resources.settings_sound_label
import com.keepstraight.desktop.generated.resources.settings_sound_subtitle
import com.keepstraight.desktop.generated.resources.settings_start_hidden_label
import com.keepstraight.desktop.generated.resources.settings_start_hidden_subtitle
import com.keepstraight.desktop.generated.resources.settings_start_hidden_unavailable
import com.keepstraight.desktop.generated.resources.settings_time_minutes
import com.keepstraight.desktop.generated.resources.settings_time_seconds
import com.keepstraight.desktop.generated.resources.wizard_hub_body_else
import com.keepstraight.desktop.generated.resources.wizard_hub_body_not_setup
import com.keepstraight.desktop.generated.resources.wizard_hub_body_paired
import com.keepstraight.desktop.generated.resources.wizard_hub_intro
import com.keepstraight.desktop.generated.resources.wizard_hub_link_degraded
import com.keepstraight.desktop.generated.resources.wizard_hub_link_failed
import com.keepstraight.desktop.generated.resources.wizard_hub_link_not_setup
import com.keepstraight.desktop.generated.resources.wizard_hub_link_paired
import com.keepstraight.desktop.generated.resources.wizard_install_on_device
import com.keepstraight.desktop.generated.resources.wizard_install_phone_body
import com.keepstraight.desktop.generated.resources.wizard_install_phone_on_phone_body
import com.keepstraight.desktop.generated.resources.wizard_install_phone_on_phone_title
import com.keepstraight.desktop.generated.resources.wizard_install_phone_qr_cd
import com.keepstraight.desktop.generated.resources.wizard_install_phone_settings_hint
import com.keepstraight.desktop.generated.resources.wizard_install_phone_skip
import com.keepstraight.desktop.generated.resources.wizard_install_phone_title
import com.keepstraight.desktop.generated.resources.wizard_install_watch_address_label
import com.keepstraight.desktop.generated.resources.wizard_install_watch_address_placeholder
import com.keepstraight.desktop.generated.resources.wizard_install_watch_body
import com.keepstraight.desktop.generated.resources.wizard_install_watch_code_label
import com.keepstraight.desktop.generated.resources.wizard_install_watch_code_placeholder
import com.keepstraight.desktop.generated.resources.wizard_install_watch_on_watch_body
import com.keepstraight.desktop.generated.resources.wizard_install_watch_on_watch_title
import com.keepstraight.desktop.generated.resources.wizard_install_watch_pairing_note
import com.keepstraight.desktop.generated.resources.wizard_install_watch_title
import com.keepstraight.desktop.generated.resources.wizard_link_body
import com.keepstraight.desktop.generated.resources.wizard_link_need_phone_body
import com.keepstraight.desktop.generated.resources.wizard_link_need_phone_title
import com.keepstraight.desktop.generated.resources.wizard_link_qr_cd
import com.keepstraight.desktop.generated.resources.wizard_link_qr_error_body
import com.keepstraight.desktop.generated.resources.wizard_link_qr_error_title
import com.keepstraight.desktop.generated.resources.wizard_link_title
import com.keepstraight.desktop.generated.resources.wizard_link_waiting
import com.keepstraight.desktop.generated.resources.wizard_phone_watch_title
import com.keepstraight.desktop.generated.resources.wizard_step_install_phone
import com.keepstraight.desktop.generated.resources.wizard_step_install_watch
import com.keepstraight.desktop.generated.resources.wizard_step_link_phone
import com.keepstraight.desktop.generated.resources.wizard_welcome_body
import com.keepstraight.desktop.generated.resources.wizard_welcome_companion
import com.keepstraight.desktop.generated.resources.wizard_welcome_title
import org.jetbrains.compose.resources.stringResource

/** Centralized desktop UI copy — keeps Compose resource extension imports in one place. */
object DesktopStrings {
    @Composable fun appName() = stringResource(Res.string.app_name)

    @Composable fun appTagline() = stringResource(Res.string.app_tagline)

    @Composable fun settingsTitle() = stringResource(Res.string.settings)

    @Composable fun actionStart() = stringResource(Res.string.action_start)

    @Composable fun actionStop() = stringResource(Res.string.action_stop)

    @Composable fun actionCalibrate() = stringResource(Res.string.action_calibrate)

    @Composable fun actionHideToTray() = stringResource(Res.string.action_hide_to_tray)

    @Composable fun actionDone() = stringResource(Res.string.action_done)

    @Composable fun actionBack() = stringResource(Res.string.action_back)

    @Composable fun actionAccept() = stringResource(Res.string.action_accept)

    @Composable fun actionQuit() = stringResource(Res.string.action_quit)

    @Composable fun actionClose() = stringResource(Res.string.action_close)

    @Composable fun actionCancel() = stringResource(Res.string.action_cancel)

    @Composable fun actionRefresh() = stringResource(Res.string.action_refresh)

    @Composable fun actionContinue() = stringResource(Res.string.action_continue)

    @Composable fun actionTryAgain() = stringResource(Res.string.action_try_again)

    @Composable fun actionNewQr() = stringResource(Res.string.action_new_qr)

    @Composable fun actionManage() = stringResource(Res.string.action_manage)

    @Composable fun actionReconnect() = stringResource(Res.string.action_reconnect)

    @Composable fun actionChecking() = stringResource(Res.string.action_checking)

    @Composable fun actionSetupPhoneWatch() = stringResource(Res.string.action_setup_phone_watch)

    @Composable fun actionSetupAgain() = stringResource(Res.string.action_setup_again)

    @Composable fun actionScanNewQr() = stringResource(Res.string.action_scan_new_qr)

    @Composable fun actionRetryCamera() = stringResource(Res.string.action_retry_camera)

    @Composable fun actionRefreshCameras() = stringResource(Res.string.action_refresh_cameras)

    @Composable fun actionUnlinkPhone() = stringResource(Res.string.action_unlink_phone)

    @Composable fun actionFixPhoneLink() = stringResource(Res.string.action_fix_phone_link)

    @Composable fun actionSkipForNow() = stringResource(Res.string.action_skip_for_now)

    @Composable fun actionStopWaiting() = stringResource(Res.string.action_stop_waiting)

    @Composable fun actionShowNewCode() = stringResource(Res.string.action_show_new_code)

    @Composable fun actionSearchAgain() = stringResource(Res.string.action_search_again)

    @Composable fun actionConnectInstall() = stringResource(Res.string.action_connect_install)

    @Composable fun actionEnterAddressManually() = stringResource(Res.string.action_enter_address_manually)

    @Composable fun actionSkipPhoneWatch() = stringResource(Res.string.action_skip_phone_watch)

    @Composable fun actionSendTestNotification() = stringResource(Res.string.action_send_test_notification)

    @Composable fun actionCancelQr() = stringResource(Res.string.action_cancel_qr)

    @Composable fun actionShowQrLinkPhone() = stringResource(Res.string.action_show_qr_link_phone)

    @Composable fun actionHideWindowNow() = stringResource(Res.string.action_hide_window_now)

    @Composable fun actionQuitApp() = stringResource(Res.string.action_quit_app)

    @Composable fun actionDoneLinking() = stringResource(Res.string.action_done_linking)

    @Composable fun actionCaptureSlumpedPose() = stringResource(Res.string.action_capture_slumped_pose)

    @Composable fun actionRedoErect() = stringResource(Res.string.action_redo_erect)

    @Composable fun actionRecalibrateBoth() = stringResource(Res.string.action_recalibrate_both)

    @Composable fun actionCaptureErectPose() = stringResource(Res.string.action_capture_erect_pose)

    @Composable fun actionRetrySlumpedPose() = stringResource(Res.string.action_retry_slumped_pose)

    @Composable fun homePrivacyNote() = stringResource(Res.string.home_privacy_note)

    @Composable fun homePresenceSlump(presence: String, slumpPercent: Int) =
        stringResource(Res.string.home_presence_slump, presence, slumpPercent)

    @Composable fun homeSlumpScore(slumpPercent: Int) =
        stringResource(Res.string.home_slump_score, slumpPercent)

    @Composable fun bridgeNotLinkedTitle() = stringResource(Res.string.bridge_not_linked_title)

    @Composable fun bridgeNotLinkedBody() = stringResource(Res.string.bridge_not_linked_body)

    @Composable fun bridgeLinkedTitle() = stringResource(Res.string.bridge_linked_title)

    @Composable fun bridgeConnected() = stringResource(Res.string.bridge_connected)

    @Composable fun bridgeConnectedTo(host: String) = stringResource(Res.string.bridge_connected_to, host)

    @Composable fun bridgeDegradedTitle() = stringResource(Res.string.bridge_degraded_title)

    @Composable fun bridgeDegradedBody() = stringResource(Res.string.bridge_degraded_body)

    @Composable fun bridgeFailedTitle() = stringResource(Res.string.bridge_failed_title)

    @Composable fun bridgeFailedBody() = stringResource(Res.string.bridge_failed_body)

    @Composable fun consentCameraBody() = stringResource(Res.string.consent_camera_body)

    @Composable fun consentOfflineBody() = stringResource(Res.string.consent_offline_body)

    @Composable fun calibrationPreviewCd() = stringResource(Res.string.calibration_preview_cd)

    @Composable fun calibrationStartingCamera() = stringResource(Res.string.calibration_starting_camera)

    @Composable fun calibrationTitle() = stringResource(Res.string.calibration_title)

    @Composable fun calibrationHoldStill() = stringResource(Res.string.calibration_hold_still)

    @Composable fun calibrationIssueCamera() = stringResource(Res.string.calibration_issue_camera)

    @Composable fun calibrationIssueModelMissing() = stringResource(Res.string.calibration_issue_model_missing)

    @Composable fun calibrationIssuePosesSimilar() = stringResource(Res.string.calibration_issue_poses_similar)

    @Composable fun calibrationIssueNoPose() = stringResource(Res.string.calibration_issue_no_pose)

    @Composable fun calibrationIssueNeedsErect() = stringResource(Res.string.calibration_issue_needs_erect)

    @Composable fun calibrationPhaseHoldGood() = stringResource(Res.string.calibration_phase_hold_good)

    @Composable fun calibrationPhaseHoldSlouch() = stringResource(Res.string.calibration_phase_hold_slouch)

    @Composable fun calibrationStep2Slumped() = stringResource(Res.string.calibration_step2_slumped)

    @Composable fun calibrationComplete() = stringResource(Res.string.calibration_complete)

    @Composable fun calibrationStep1Erect() = stringResource(Res.string.calibration_step1_erect)

    @Composable fun calibrationBodyCaptureErect() = stringResource(Res.string.calibration_body_capture_erect)

    @Composable fun calibrationBodyCaptureSlump() = stringResource(Res.string.calibration_body_capture_slump)

    @Composable fun calibrationBodyAfterErect() = stringResource(Res.string.calibration_body_after_erect)

    @Composable fun calibrationBodyDefault() = stringResource(Res.string.calibration_body_default)

    @Composable fun settingsSectionAlerts() = stringResource(Res.string.settings_section_alerts)

    @Composable fun settingsSoundLabel() = stringResource(Res.string.settings_sound_label)

    @Composable fun settingsSoundSubtitle() = stringResource(Res.string.settings_sound_subtitle)

    @Composable fun settingsNotificationLabel() = stringResource(Res.string.settings_notification_label)

    @Composable fun settingsNotificationSubtitleSupported() =
        stringResource(Res.string.settings_notification_subtitle_supported)

    @Composable fun settingsNotificationSubtitleUnsupported() =
        stringResource(Res.string.settings_notification_subtitle_unsupported)

    @Composable fun settingsSectionPhoneWatch() = stringResource(Res.string.settings_section_phone_watch)

    @Composable fun settingsQrLinkCd() = stringResource(Res.string.settings_qr_link_cd)

    @Composable fun settingsSectionCamera() = stringResource(Res.string.settings_section_camera)

    @Composable fun settingsNoCamera() = stringResource(Res.string.settings_no_camera)

    @Composable fun settingsSectionSensitivity() = SharedStrings.settingsSectionSensitivity()

    @Composable fun settingsSectionSensitivityFromPhone() =
        SharedStrings.settingsSectionSensitivityFromPhone()

    @Composable
    fun sensitivityLabel(level: SensitivityLevel): String = SharedStrings.sensitivityLabel(level)

    /** Maps JvmCameraFrameSource fallback names (Camera 0) to localized labels. */
    @Composable
    fun cameraDisplayName(rawName: String): String {
        val match = Regex("""^Camera (\d+)$""").matchEntire(rawName) ?: return rawName
        return stringResource(Res.string.settings_camera_default_name, match.groupValues[1].toInt() + 1)
    }

    @Composable fun settingsAlertTiming(first: String, repeat: String) =
        SharedStrings.settingsAlertTiming(first, repeat)

    @Composable fun settingsPhoneOwnsTimers() = SharedStrings.settingsPhoneOwnsTimers()

    @Composable fun settingsSectionGeneral() = stringResource(Res.string.settings_section_general)

    @Composable fun settingsLowPowerLabel() = stringResource(Res.string.settings_low_power_label)

    @Composable fun settingsOpenAtLoginLabel() = stringResource(Res.string.settings_open_at_login_label)

    @Composable fun settingsOpenAtLoginSubtitle() = stringResource(Res.string.settings_open_at_login_subtitle)

    @Composable fun settingsOpenAtLoginUnavailable() = stringResource(Res.string.settings_open_at_login_unavailable)

    @Composable fun settingsStartHiddenLabel() = stringResource(Res.string.settings_start_hidden_label)

    @Composable fun settingsStartHiddenSubtitle() = stringResource(Res.string.settings_start_hidden_subtitle)

    @Composable fun settingsStartHiddenUnavailable() = stringResource(Res.string.settings_start_hidden_unavailable)

    @Composable fun settingsBridgeNotLinked() = stringResource(Res.string.settings_bridge_not_linked)

    @Composable fun settingsBridgeLinked(host: String) = stringResource(Res.string.settings_bridge_linked, host)

    @Composable fun settingsBridgeDegraded(host: String) = stringResource(Res.string.settings_bridge_degraded, host)

    @Composable fun settingsBridgeFailed() = stringResource(Res.string.settings_bridge_failed)

    @Composable fun settingsTimeSeconds(seconds: Long) = stringResource(Res.string.settings_time_seconds, seconds)

    @Composable fun settingsTimeMinutes(minutes: Long) = stringResource(Res.string.settings_time_minutes, minutes)

    @Composable fun wizardWelcomeTitle() = stringResource(Res.string.wizard_welcome_title)

    @Composable fun wizardWelcomeBody() = stringResource(Res.string.wizard_welcome_body)

    @Composable fun wizardWelcomeCompanion() = stringResource(Res.string.wizard_welcome_companion)

    @Composable fun wizardPhoneWatchTitle() = stringResource(Res.string.wizard_phone_watch_title)

    @Composable fun wizardHubIntro() = stringResource(Res.string.wizard_hub_intro)

    @Composable fun wizardHubLinkPaired() = stringResource(Res.string.wizard_hub_link_paired)

    @Composable fun wizardHubLinkDegraded() = stringResource(Res.string.wizard_hub_link_degraded)

    @Composable fun wizardHubLinkFailed() = stringResource(Res.string.wizard_hub_link_failed)

    @Composable fun wizardHubLinkNotSetup() = stringResource(Res.string.wizard_hub_link_not_setup)

    @Composable fun wizardHubBodyPaired() = stringResource(Res.string.wizard_hub_body_paired)

    @Composable fun wizardHubBodyNotSetup() = stringResource(Res.string.wizard_hub_body_not_setup)

    @Composable fun wizardHubBodyElse() = stringResource(Res.string.wizard_hub_body_else)

    @Composable fun wizardStepInstallWatch() = stringResource(Res.string.wizard_step_install_watch)

    @Composable fun wizardStepInstallPhone() = stringResource(Res.string.wizard_step_install_phone)

    @Composable fun wizardStepLinkPhone() = stringResource(Res.string.wizard_step_link_phone)

    @Composable fun wizardLinkTitle() = stringResource(Res.string.wizard_link_title)

    @Composable fun wizardLinkBody() = stringResource(Res.string.wizard_link_body)

    @Composable fun wizardLinkNeedPhoneTitle() = stringResource(Res.string.wizard_link_need_phone_title)

    @Composable fun wizardLinkNeedPhoneBody() = stringResource(Res.string.wizard_link_need_phone_body)

    @Composable fun wizardLinkWaiting() = stringResource(Res.string.wizard_link_waiting)

    @Composable fun wizardLinkQrErrorTitle() = stringResource(Res.string.wizard_link_qr_error_title)

    @Composable fun wizardLinkQrErrorBody() = stringResource(Res.string.wizard_link_qr_error_body)

    @Composable fun wizardLinkQrCd() = stringResource(Res.string.wizard_link_qr_cd)

    @Composable fun wizardInstallPhoneTitle() = stringResource(Res.string.wizard_install_phone_title)

    @Composable fun wizardInstallPhoneBody() = stringResource(Res.string.wizard_install_phone_body)

    @Composable fun wizardInstallPhoneOnPhoneTitle() = stringResource(Res.string.wizard_install_phone_on_phone_title)

    @Composable fun wizardInstallPhoneOnPhoneBody() = stringResource(Res.string.wizard_install_phone_on_phone_body)

    @Composable fun wizardInstallPhoneQrCd() = stringResource(Res.string.wizard_install_phone_qr_cd)

    @Composable fun wizardInstallPhoneSkip() = stringResource(Res.string.wizard_install_phone_skip)

    @Composable fun wizardInstallPhoneSettingsHint() = stringResource(Res.string.wizard_install_phone_settings_hint)

    @Composable fun wizardInstallWatchTitle() = stringResource(Res.string.wizard_install_watch_title)

    @Composable fun wizardInstallWatchBody() = stringResource(Res.string.wizard_install_watch_body)

    @Composable fun wizardInstallWatchOnWatchTitle() = stringResource(Res.string.wizard_install_watch_on_watch_title)

    @Composable fun wizardInstallWatchOnWatchBody() = stringResource(Res.string.wizard_install_watch_on_watch_body)

    @Composable fun wizardInstallWatchAddressLabel() = stringResource(Res.string.wizard_install_watch_address_label)

    @Composable fun wizardInstallWatchAddressPlaceholder() =
        stringResource(Res.string.wizard_install_watch_address_placeholder)

    @Composable fun wizardInstallWatchCodeLabel() = stringResource(Res.string.wizard_install_watch_code_label)

    @Composable fun wizardInstallWatchCodePlaceholder() =
        stringResource(Res.string.wizard_install_watch_code_placeholder)

    @Composable fun wizardInstallWatchPairingNote() = stringResource(Res.string.wizard_install_watch_pairing_note)

    @Composable fun wizardInstallOnDevice(name: String) = stringResource(Res.string.wizard_install_on_device, name)
}
