package com.keepstraight.wear.ui.model

import androidx.compose.ui.graphics.Color
import com.keepstraight.shared.presentation.MonitoringState
import com.keepstraight.wear.R
import com.keepstraight.wear.ui.theme.WearColors

data class WatchStatusPresentation(
    val titleRes: Int,
    val hintRes: Int,
    val accent: Color,
)

fun watchStatusPresentation(
    state: MonitoringState,
    isCalibrating: Boolean,
): WatchStatusPresentation {
    if (isCalibrating) {
        return WatchStatusPresentation(
            titleRes = R.string.status_calibrating,
            hintRes = R.string.status_calibrating_hint,
            accent = WearColors.statusCalibrating,
        )
    }
    return when (state) {
        MonitoringState.ACTIVE -> WatchStatusPresentation(
            titleRes = R.string.status_monitoring,
            hintRes = R.string.status_monitoring_hint,
            accent = WearColors.statusMonitoring,
        )
        MonitoringState.ALERTS_PAUSED -> WatchStatusPresentation(
            titleRes = R.string.status_alerts_paused,
            hintRes = R.string.status_alerts_paused_hint,
            accent = WearColors.statusAlertsPaused,
        )
        MonitoringState.ALGORITHM_OFF -> WatchStatusPresentation(
            titleRes = R.string.status_algorithm_off,
            hintRes = R.string.status_algorithm_off_hint,
            accent = WearColors.statusAlgorithmOff,
        )
        MonitoringState.NOT_SITTING -> WatchStatusPresentation(
            titleRes = R.string.status_not_sitting,
            hintRes = R.string.status_not_sitting_hint,
            accent = WearColors.statusNotSitting,
        )
        MonitoringState.NOT_WORN -> WatchStatusPresentation(
            titleRes = R.string.status_not_worn,
            hintRes = R.string.status_not_worn_hint,
            accent = WearColors.statusNotWorn,
        )
        MonitoringState.PHONE_RETRY -> WatchStatusPresentation(
            titleRes = R.string.status_phone_retry,
            hintRes = R.string.status_phone_retry_hint,
            accent = WearColors.statusPhoneRetry,
        )
        MonitoringState.PHONE_DISCONNECTED_PAUSED -> WatchStatusPresentation(
            titleRes = R.string.status_phone_paused,
            hintRes = R.string.status_phone_paused_hint,
            accent = WearColors.statusPhonePaused,
        )
        MonitoringState.DND_ACTIVE -> WatchStatusPresentation(
            titleRes = R.string.status_dnd,
            hintRes = R.string.status_dnd_hint,
            accent = WearColors.statusDnd,
        )
    }
}
