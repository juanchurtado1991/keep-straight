package com.keepstraight.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.keepstraight.R

@Composable
fun formatEventDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remaining = seconds % 60
    return if (minutes > 0) {
        stringResource(R.string.duration_minutes_seconds, minutes, remaining)
    } else {
        stringResource(R.string.duration_seconds, remaining)
    }
}

@Composable
fun formatChartDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) {
        stringResource(R.string.duration_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.duration_minutes_only, minutes)
    }
}

@Composable
fun formatSensitivityDurationLabel(seconds: Int): String = when {
    seconds < 60 -> stringResource(R.string.duration_seconds, seconds)
    seconds % 60 == 0 -> stringResource(R.string.duration_minutes_long, seconds / 60)
    else -> stringResource(
        R.string.duration_minutes_seconds,
        seconds / 60,
        seconds % 60,
    )
}
