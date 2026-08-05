package com.keepstraight.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.MaterialTheme
import com.keepstraight.shared.presentation.MonitoringState
import com.keepstraight.wear.ui.components.MonitoringStatusIndicator
import com.keepstraight.wear.ui.model.watchStatusPresentation
import com.keepstraight.wear.ui.theme.WearColors

@Composable
fun MonitoringScreen(
    state: MonitoringState,
    isCalibrating: Boolean,
    flashVisible: Boolean,
) {
    val presentation = watchStatusPresentation(state, isCalibrating)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (flashVisible) {
                    Modifier.background(WearColors.flashOverlay)
                } else {
                    Modifier.background(MaterialTheme.colors.background)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        MonitoringStatusIndicator(
            presentation = presentation,
            flashVisible = flashVisible,
        )
    }
}
