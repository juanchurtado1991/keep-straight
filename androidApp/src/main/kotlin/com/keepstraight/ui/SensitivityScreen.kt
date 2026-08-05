package com.keepstraight.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.keepstraight.presentation.settings.SettingsViewModel
import com.keepstraight.sharedui.i18n.SharedStrings
import com.keepstraight.sharedui.sensitivity.SharedSensitivityScreen
import com.keepstraight.ui.components.KeepStraightTopBar

@Composable
fun SensitivityScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val sensitivity by viewModel.sensitivity.collectAsState()
    val slumpMs by viewModel.slumpDurationThresholdMs.collectAsState()
    val repeatMs by viewModel.repeatAlertIntervalMs.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            KeepStraightTopBar(
                title = SharedStrings.sensitivityTitle(),
                onBack = onBack,
            )
        },
    ) { padding ->
        SharedSensitivityScreen(
            sensitivity = sensitivity,
            slumpDurationMs = slumpMs,
            repeatAlertMs = repeatMs,
            showTimingSliders = true,
            onSensitivityChange = viewModel::setSensitivity,
            onSlumpTimingChange = viewModel::setSlumpTiming,
            modifier = Modifier.padding(padding),
        )
    }
}
