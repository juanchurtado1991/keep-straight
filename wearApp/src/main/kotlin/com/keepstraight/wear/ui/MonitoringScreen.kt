package com.keepstraight.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.keepstraight.shared.presentation.MonitoringState
import com.keepstraight.wear.R

@Composable
fun MonitoringScreen(
    state: MonitoringState,
    isCalibrating: Boolean,
    flashVisible: Boolean,
) {
    // Posture itself is scored on the desktop, so the watch only shows its own readiness.
    val presentation = watchStatusPresentation(state, isCalibrating)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (flashVisible) {
                    Modifier.background(Color.White.copy(alpha = 0.9f))
                } else {
                    Modifier.background(MaterialTheme.colors.background)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (flashVisible) Color.DarkGray else presentation.accent),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.app_name),
                color = if (flashVisible) {
                    Color.DarkGray.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colors.onBackground.copy(alpha = 0.65f)
                },
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(presentation.titleRes),
                textAlign = TextAlign.Center,
                color = if (flashVisible) Color.DarkGray else MaterialTheme.colors.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(presentation.hintRes),
                textAlign = TextAlign.Center,
                color = if (flashVisible) {
                    Color.DarkGray.copy(alpha = 0.75f)
                } else {
                    MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
                },
                fontSize = 11.sp,
                maxLines = 3,
            )
        }
    }
}

private data class WatchStatusPresentation(
    val titleRes: Int,
    val hintRes: Int,
    val accent: Color,
)

private fun watchStatusPresentation(
    state: MonitoringState,
    isCalibrating: Boolean,
): WatchStatusPresentation {
    if (isCalibrating) {
        return WatchStatusPresentation(
            titleRes = R.string.status_calibrating,
            hintRes = R.string.status_calibrating_hint,
            accent = Color(0xFF64B5F6),
        )
    }
    return when (state) {
        MonitoringState.ACTIVE -> WatchStatusPresentation(
            titleRes = R.string.status_monitoring,
            hintRes = R.string.status_monitoring_hint,
            accent = Color(0xFF66BB6A),
        )
        MonitoringState.ALERTS_PAUSED -> WatchStatusPresentation(
            titleRes = R.string.status_alerts_paused,
            hintRes = R.string.status_alerts_paused_hint,
            accent = Color(0xFFFFB74D),
        )
        MonitoringState.ALGORITHM_OFF -> WatchStatusPresentation(
            titleRes = R.string.status_algorithm_off,
            hintRes = R.string.status_algorithm_off_hint,
            accent = Color(0xFF90A4AE),
        )
        MonitoringState.NOT_SITTING -> WatchStatusPresentation(
            titleRes = R.string.status_not_sitting,
            hintRes = R.string.status_not_sitting_hint,
            accent = Color(0xFF4FC3F7),
        )
        MonitoringState.NOT_WORN -> WatchStatusPresentation(
            titleRes = R.string.status_not_worn,
            hintRes = R.string.status_not_worn_hint,
            accent = Color(0xFFE57373),
        )
        MonitoringState.PHONE_RETRY -> WatchStatusPresentation(
            titleRes = R.string.status_phone_retry,
            hintRes = R.string.status_phone_retry_hint,
            accent = Color(0xFFFFB74D),
        )
        MonitoringState.PHONE_DISCONNECTED_PAUSED -> WatchStatusPresentation(
            titleRes = R.string.status_phone_paused,
            hintRes = R.string.status_phone_paused_hint,
            accent = Color(0xFFE57373),
        )
        MonitoringState.DND_ACTIVE -> WatchStatusPresentation(
            titleRes = R.string.status_dnd,
            hintRes = R.string.status_dnd_hint,
            accent = Color(0xFFBA68C8),
        )
    }
}
