package com.keepstraight.sharedui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.sharedui.sensitivity.SharedSensitivityScreen

data class SensitivityScreenRoute(
    val sensitivity: SensitivityLevel,
    val slumpDurationMs: Long,
    val repeatAlertMs: Long,
    val showTimingSliders: Boolean,
    val sensitivityEnabled: Boolean = true,
    val settingsFromPhone: Boolean = false,
    val onSensitivityChange: (SensitivityLevel) -> Unit,
    val onSlumpTimingChange: (Long, Long) -> Unit,
    val contentModifier: Modifier = Modifier,
    val header: @Composable () -> Unit = {},
) : Screen {
    @Composable
    override fun Content() {
        SharedSensitivityScreen(
            sensitivity = sensitivity,
            slumpDurationMs = slumpDurationMs,
            repeatAlertMs = repeatAlertMs,
            showTimingSliders = showTimingSliders,
            onSensitivityChange = onSensitivityChange,
            onSlumpTimingChange = onSlumpTimingChange,
            modifier = contentModifier,
            sensitivityEnabled = sensitivityEnabled,
            settingsFromPhone = settingsFromPhone,
            header = header,
        )
    }
}
