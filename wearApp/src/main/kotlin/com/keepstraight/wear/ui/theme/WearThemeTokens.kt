package com.keepstraight.wear.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object WearDimens {
    val screenPaddingHorizontal = 28.dp
    val screenPaddingVertical = 22.dp
    val statusDotSize = 10.dp
    val titleGap = 10.dp
    val headlineGap = 6.dp
    val hintGap = 4.dp
}

object WearTypography {
    val appName = 11.sp
    val title = 14.sp
    val hint = 11.sp
}

object WearColors {
    val flashOverlay = Color.White.copy(alpha = 0.9f)
    val flashText = Color.DarkGray
    val flashTextMuted = Color.DarkGray.copy(alpha = 0.7f)
    val flashTextHint = Color.DarkGray.copy(alpha = 0.75f)

    val statusCalibrating = Color(0xFF64B5F6)
    val statusMonitoring = Color(0xFF66BB6A)
    val statusAlertsPaused = Color(0xFFFFB74D)
    val statusAlgorithmOff = Color(0xFF90A4AE)
    val statusNotSitting = Color(0xFF4FC3F7)
    val statusNotWorn = Color(0xFFE57373)
    val statusPhoneRetry = Color(0xFFFFB74D)
    val statusPhonePaused = Color(0xFFE57373)
    val statusDnd = Color(0xFFBA68C8)

    val onBackgroundMuted = 0.65f
    val onBackgroundHint = 0.7f
}
