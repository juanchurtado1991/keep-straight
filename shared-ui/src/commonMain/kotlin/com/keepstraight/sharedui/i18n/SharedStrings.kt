package com.keepstraight.sharedui.i18n

import androidx.compose.runtime.Composable
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.sharedui.generated.resources.Res
import com.keepstraight.sharedui.generated.resources.action_back
import com.keepstraight.sharedui.generated.resources.action_done
import com.keepstraight.sharedui.generated.resources.duration_minutes_long
import com.keepstraight.sharedui.generated.resources.duration_minutes_seconds
import com.keepstraight.sharedui.generated.resources.duration_seconds
import com.keepstraight.sharedui.generated.resources.settings_alert_timing
import com.keepstraight.sharedui.generated.resources.settings_phone_owns_timers
import com.keepstraight.sharedui.generated.resources.settings_section_sensitivity
import com.keepstraight.sharedui.generated.resources.settings_section_sensitivity_from_phone
import com.keepstraight.sharedui.generated.resources.settings_time_minutes
import com.keepstraight.sharedui.generated.resources.settings_time_seconds
import com.keepstraight.sharedui.generated.resources.sensitivity_normal
import com.keepstraight.sharedui.generated.resources.sensitivity_relaxed
import com.keepstraight.sharedui.generated.resources.sensitivity_repeat_title
import com.keepstraight.sharedui.generated.resources.sensitivity_repeat_value
import com.keepstraight.sharedui.generated.resources.sensitivity_slump_delay_hint
import com.keepstraight.sharedui.generated.resources.sensitivity_slump_delay_title
import com.keepstraight.sharedui.generated.resources.sensitivity_slump_delay_value
import com.keepstraight.sharedui.generated.resources.sensitivity_strict
import com.keepstraight.sharedui.generated.resources.sensitivity_title
import org.jetbrains.compose.resources.stringResource

object SharedStrings {
    @Composable fun actionDone() = stringResource(Res.string.action_done)

    @Composable fun actionBack() = stringResource(Res.string.action_back)

    @Composable fun sensitivityTitle() = stringResource(Res.string.sensitivity_title)

    @Composable fun sensitivityLabel(level: SensitivityLevel): String = when (level) {
        SensitivityLevel.STRICT -> stringResource(Res.string.sensitivity_strict)
        SensitivityLevel.NORMAL -> stringResource(Res.string.sensitivity_normal)
        SensitivityLevel.RELAXED -> stringResource(Res.string.sensitivity_relaxed)
    }

    @Composable fun sensitivitySlumpDelayTitle() = stringResource(Res.string.sensitivity_slump_delay_title)

    @Composable fun sensitivitySlumpDelayValue(durationLabel: String) =
        stringResource(Res.string.sensitivity_slump_delay_value, durationLabel)

    @Composable fun sensitivitySlumpDelayHint() = stringResource(Res.string.sensitivity_slump_delay_hint)

    @Composable fun sensitivityRepeatTitle() = stringResource(Res.string.sensitivity_repeat_title)

    @Composable fun sensitivityRepeatValue(seconds: Int) =
        stringResource(Res.string.sensitivity_repeat_value, seconds)

    @Composable fun settingsSectionSensitivity() = stringResource(Res.string.settings_section_sensitivity)

    @Composable fun settingsSectionSensitivityFromPhone() =
        stringResource(Res.string.settings_section_sensitivity_from_phone)

    @Composable fun settingsAlertTiming(first: String, repeat: String) =
        stringResource(Res.string.settings_alert_timing, first, repeat)

    @Composable fun settingsPhoneOwnsTimers() = stringResource(Res.string.settings_phone_owns_timers)

    @Composable fun settingsTimeSeconds(seconds: Int) =
        stringResource(Res.string.settings_time_seconds, seconds)

    @Composable fun settingsTimeMinutes(minutes: Int) =
        stringResource(Res.string.settings_time_minutes, minutes)

    @Composable
    fun formatSensitivityDurationLabel(seconds: Int): String = when {
        seconds < 60 -> stringResource(Res.string.duration_seconds, seconds)
        seconds % 60 == 0 -> stringResource(Res.string.duration_minutes_long, seconds / 60)
        else -> stringResource(
            Res.string.duration_minutes_seconds,
            seconds / 60,
            seconds % 60,
        )
    }

    @Composable
    fun formatCompactDuration(ms: Long): String {
        val totalSeconds = (ms / 1000).toInt()
        return if (totalSeconds < 60) {
            settingsTimeSeconds(totalSeconds)
        } else {
            settingsTimeMinutes(totalSeconds / 60)
        }
    }
}
