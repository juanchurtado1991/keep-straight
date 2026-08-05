package com.keepstraight.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.keepstraight.R
import com.keepstraight.data.UserPreferencesRepository
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.ui.common.formatSensitivityDurationLabel
import com.keepstraight.ui.components.KeepStraightTopBar
import com.keepstraight.ui.theme.PhoneCard
import com.keepstraight.ui.theme.PhoneDimens
import com.keepstraight.ui.theme.PhonePage
import com.keepstraight.presentation.settings.SettingsViewModel
import kotlin.math.roundToInt

@Composable
fun SensitivityScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val sensitivity by viewModel.sensitivity.collectAsState()
    val slumpMs by viewModel.slumpDurationThresholdMs.collectAsState()
    val repeatMs by viewModel.repeatAlertIntervalMs.collectAsState()

    var slumpSeconds by remember { mutableFloatStateOf(slumpMs / 1000f) }
    var repeatSeconds by remember { mutableFloatStateOf(repeatMs / 1000f) }

    LaunchedEffect(slumpMs) { slumpSeconds = slumpMs / 1000f }
    LaunchedEffect(repeatMs) { repeatSeconds = repeatMs / 1000f }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            KeepStraightTopBar(
                title = stringResource(R.string.sensitivity_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        PhonePage(modifier = Modifier.padding(padding)) {
            PhoneCard {
                Text(
                    text = stringResource(R.string.sensitivity_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                SensitivityLevel.entries.forEach { level ->
                    val label = when (level) {
                        SensitivityLevel.STRICT -> stringResource(R.string.sensitivity_strict)
                        SensitivityLevel.NORMAL -> stringResource(R.string.sensitivity_normal)
                        SensitivityLevel.RELAXED -> stringResource(R.string.sensitivity_relaxed)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = sensitivity == level,
                                onClick = { viewModel.setSensitivity(level) },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PhoneDimens.rowGap),
                    ) {
                        RadioButton(
                            selected = sensitivity == level,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            PhoneCard {
                Text(
                    text = stringResource(R.string.sensitivity_slump_delay_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(
                        R.string.sensitivity_slump_delay_value,
                        formatSensitivityDurationLabel(slumpSeconds.roundToInt()),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.sensitivity_slump_delay_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = slumpSeconds,
                    onValueChange = { slumpSeconds = snapSlumpSeconds(it) },
                    valueRange = (UserPreferencesRepository.MIN_SLUMP_DURATION_MS / 1000f)..
                        (UserPreferencesRepository.MAX_SLUMP_DURATION_MS / 1000f),
                    onValueChangeFinished = {
                        viewModel.setSlumpTiming(
                            slumpDurationThresholdMs = (slumpSeconds * 1000f).toLong(),
                            repeatAlertIntervalMs = (repeatSeconds * 1000f).toLong(),
                        )
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                Text(
                    text = stringResource(R.string.sensitivity_repeat_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(
                        R.string.sensitivity_repeat_value,
                        repeatSeconds.roundToInt(),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Slider(
                    value = repeatSeconds,
                    onValueChange = { repeatSeconds = it.roundToInt().toFloat().coerceIn(2f, 30f) },
                    valueRange = (UserPreferencesRepository.MIN_REPEAT_ALERT_MS / 1000f)..
                        (UserPreferencesRepository.MAX_REPEAT_ALERT_MS / 1000f),
                    steps = 27,
                    onValueChangeFinished = {
                        viewModel.setSlumpTiming(
                            slumpDurationThresholdMs = (slumpSeconds * 1000f).toLong(),
                            repeatAlertIntervalMs = (repeatSeconds * 1000f).toLong(),
                        )
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
    }
}

private fun snapSlumpSeconds(raw: Float): Float {
    val seconds = raw.roundToInt()
    val snapped = when {
        seconds <= 5 -> 5
        seconds <= 10 -> 10
        seconds <= 15 -> 15
        seconds <= 30 -> 30
        seconds <= 60 -> 60
        seconds <= 120 -> 120
        seconds <= 180 -> 180
        else -> 300
    }
    return snapped.toFloat()
}
