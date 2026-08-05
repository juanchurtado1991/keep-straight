package com.keepstraight.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.keepstraight.R
import com.keepstraight.ui.components.KeepStraightTopBar
import com.keepstraight.ui.theme.PhoneCard
import com.keepstraight.ui.theme.PhoneDimens
import com.keepstraight.ui.theme.PhonePage
import com.keepstraight.presentation.settings.SettingsViewModel

@Composable
fun AlertSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val prefs by viewModel.alertPreferences.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            KeepStraightTopBar(
                title = stringResource(R.string.alert_settings_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        PhonePage(modifier = Modifier.padding(padding)) {
            PhoneCard {
                Text(
                    text = stringResource(R.string.alert_settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                AlertToggle(
                    label = stringResource(R.string.alert_haptic),
                    checked = prefs.hapticEnabled,
                    onCheckedChange = {
                        viewModel.setAlertPreferences(prefs.copy(hapticEnabled = it))
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AlertToggle(
                    label = stringResource(R.string.alert_visual),
                    checked = prefs.visualEnabled,
                    onCheckedChange = {
                        viewModel.setAlertPreferences(prefs.copy(visualEnabled = it))
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AlertToggle(
                    label = stringResource(R.string.alert_sound),
                    checked = prefs.soundEnabled,
                    onCheckedChange = {
                        viewModel.setAlertPreferences(prefs.copy(soundEnabled = it))
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AlertToggle(
                    label = stringResource(R.string.alert_phone_notification),
                    checked = prefs.phoneNotificationEnabled,
                    onCheckedChange = {
                        viewModel.setAlertPreferences(prefs.copy(phoneNotificationEnabled = it))
                    },
                )
            }
        }
    }
}

@Composable
private fun AlertToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(end = PhoneDimens.rowGap),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedThumbColor = MaterialTheme.colorScheme.surface,
            ),
        )
    }
}
