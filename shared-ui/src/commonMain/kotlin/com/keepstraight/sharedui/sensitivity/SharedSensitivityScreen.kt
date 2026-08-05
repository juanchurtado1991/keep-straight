package com.keepstraight.sharedui.sensitivity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.sharedui.i18n.SharedStrings
import com.keepstraight.sharedui.theme.SharedCard
import com.keepstraight.sharedui.theme.SharedDimens
import com.keepstraight.sharedui.theme.SharedPage

@Composable
fun SharedSensitivityScreen(
    sensitivity: SensitivityLevel,
    slumpDurationMs: Long,
    repeatAlertMs: Long,
    showTimingSliders: Boolean,
    onSensitivityChange: (SensitivityLevel) -> Unit,
    onSlumpTimingChange: (Long, Long) -> Unit,
    modifier: Modifier = Modifier,
    sensitivityEnabled: Boolean = true,
    settingsFromPhone: Boolean = false,
    header: @Composable () -> Unit = {},
) {
    var slumpSeconds by remember(slumpDurationMs) { mutableFloatStateOf(slumpDurationMs / 1000f) }
    var repeatSeconds by remember(repeatAlertMs) { mutableFloatStateOf(repeatAlertMs / 1000f) }

    LaunchedEffect(slumpDurationMs) { slumpSeconds = slumpDurationMs / 1000f }
    LaunchedEffect(repeatAlertMs) { repeatSeconds = repeatAlertMs / 1000f }

    Column(modifier = modifier) {
        header()
        SharedPage {
            SharedCard {
                Text(
                    text = if (settingsFromPhone) {
                        SharedStrings.settingsSectionSensitivityFromPhone()
                    } else {
                        SharedStrings.sensitivityTitle()
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                SensitivityLevelSelector(
                    selected = sensitivity,
                    onSelect = onSensitivityChange,
                    enabled = sensitivityEnabled,
                    style = if (showTimingSliders) {
                        SensitivitySelectorStyle.Radio
                    } else {
                        SensitivitySelectorStyle.Chip
                    },
                )
            }

            if (showTimingSliders) {
                SharedCard {
                    SlumpTimingEditor(
                        slumpSeconds = slumpSeconds,
                        repeatSeconds = repeatSeconds,
                        onSlumpSecondsChange = { slumpSeconds = snapSlumpSeconds(it) },
                        onRepeatSecondsChange = { repeatSeconds = snapRepeatSeconds(it) },
                        onCommit = {
                            onSlumpTimingChange(
                                (slumpSeconds * 1000f).toLong(),
                                (repeatSeconds * 1000f).toLong(),
                            )
                        },
                    )
                }
            } else {
                Text(
                    text = SharedStrings.settingsAlertTiming(
                        SharedStrings.formatCompactDuration(slumpDurationMs),
                        SharedStrings.formatCompactDuration(repeatAlertMs),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (settingsFromPhone) {
                    Text(
                        text = SharedStrings.settingsPhoneOwnsTimers(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun SharedInlineSensitivitySection(
    sensitivity: SensitivityLevel,
    slumpDurationMs: Long,
    repeatAlertMs: Long,
    settingsFromPhone: Boolean,
    sensitivityEnabled: Boolean,
    onSensitivityChange: (SensitivityLevel) -> Unit,
    sectionTitle: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SharedDimens.cardGap)) {
        sectionTitle()
        SensitivityLevelSelector(
            selected = sensitivity,
            onSelect = onSensitivityChange,
            enabled = sensitivityEnabled,
            style = SensitivitySelectorStyle.Chip,
        )
        Text(
            text = SharedStrings.settingsAlertTiming(
                SharedStrings.formatCompactDuration(slumpDurationMs),
                SharedStrings.formatCompactDuration(repeatAlertMs),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (settingsFromPhone) {
            Text(
                text = SharedStrings.settingsPhoneOwnsTimers(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
