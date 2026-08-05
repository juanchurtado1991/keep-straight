package com.keepstraight.sharedui.sensitivity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.model.SensitivityTimingLimits
import com.keepstraight.sharedui.i18n.SharedStrings
import com.keepstraight.sharedui.theme.SharedDimens
import com.keepstraight.sharedui.theme.sharedPrimaryButtonColors
import com.keepstraight.sharedui.theme.sharedSecondaryButtonColors
import com.keepstraight.sharedui.theme.sharedButtonShape
import kotlin.math.roundToInt

enum class SensitivitySelectorStyle {
    Radio,
    Chip,
}

@Composable
fun SensitivityLevelSelector(
    selected: SensitivityLevel,
    onSelect: (SensitivityLevel) -> Unit,
    enabled: Boolean = true,
    style: SensitivitySelectorStyle = SensitivitySelectorStyle.Radio,
) {
    when (style) {
        SensitivitySelectorStyle.Radio -> {
            SensitivityLevel.entries.forEach { level ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected == level,
                            onClick = { if (enabled) onSelect(level) },
                            role = Role.RadioButton,
                            enabled = enabled,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SharedDimens.rowGap),
                ) {
                    RadioButton(
                        selected = selected == level,
                        onClick = null,
                        enabled = enabled,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    Text(
                        text = SharedStrings.sensitivityLabel(level),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        SensitivitySelectorStyle.Chip -> {
            Row(horizontalArrangement = Arrangement.spacedBy(SharedDimens.rowGap)) {
                SensitivityLevel.entries.forEach { level ->
                    val isSelected = selected == level
                    if (isSelected) {
                        Button(
                            onClick = { if (enabled) onSelect(level) },
                            enabled = enabled,
                            colors = sharedPrimaryButtonColors(),
                            shape = sharedButtonShape(),
                        ) { Text(SharedStrings.sensitivityLabel(level)) }
                    } else {
                        OutlinedButton(
                            onClick = { if (enabled) onSelect(level) },
                            enabled = enabled,
                            colors = sharedSecondaryButtonColors(),
                            shape = sharedButtonShape(),
                        ) { Text(SharedStrings.sensitivityLabel(level)) }
                    }
                }
            }
        }
    }
}

@Composable
fun SlumpTimingEditor(
    slumpSeconds: Float,
    repeatSeconds: Float,
    onSlumpSecondsChange: (Float) -> Unit,
    onRepeatSecondsChange: (Float) -> Unit,
    onCommit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SharedDimens.cardGap)) {
        Text(
            text = SharedStrings.sensitivitySlumpDelayTitle(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = SharedStrings.sensitivitySlumpDelayValue(
                SharedStrings.formatSensitivityDurationLabel(slumpSeconds.roundToInt()),
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = SharedStrings.sensitivitySlumpDelayHint(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = slumpSeconds,
            onValueChange = onSlumpSecondsChange,
            valueRange = (SensitivityTimingLimits.MIN_SLUMP_DURATION_MS / 1000f)..
                (SensitivityTimingLimits.MAX_SLUMP_DURATION_MS / 1000f),
            onValueChangeFinished = onCommit,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )

        Text(
            text = SharedStrings.sensitivityRepeatTitle(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = SharedStrings.sensitivityRepeatValue(repeatSeconds.roundToInt()),
            style = MaterialTheme.typography.bodyLarge,
        )
        Slider(
            value = repeatSeconds,
            onValueChange = onRepeatSecondsChange,
            valueRange = (SensitivityTimingLimits.MIN_REPEAT_ALERT_MS / 1000f)..
                (SensitivityTimingLimits.MAX_REPEAT_ALERT_MS / 1000f),
            steps = 27,
            onValueChangeFinished = onCommit,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

fun snapSlumpSeconds(raw: Float): Float {
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

fun snapRepeatSeconds(raw: Float): Float =
    raw.roundToInt().toFloat().coerceIn(2f, 30f)
