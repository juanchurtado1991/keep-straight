package com.keepstraight.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.keepstraight.R
import com.keepstraight.ui.components.KeepStraightTopBar
import com.keepstraight.viewmodel.MainViewModel

@Composable
fun AlertSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val prefs by viewModel.alertPreferences.collectAsState()

    Scaffold(
        topBar = {
            KeepStraightTopBar(
                title = stringResource(R.string.alert_settings_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    AlertToggle(
                        label = stringResource(R.string.alert_haptic),
                        checked = prefs.hapticEnabled,
                        onCheckedChange = {
                            viewModel.setAlertPreferences(prefs.copy(hapticEnabled = it))
                        },
                    )
                    AlertToggle(
                        label = stringResource(R.string.alert_visual),
                        checked = prefs.visualEnabled,
                        onCheckedChange = {
                            viewModel.setAlertPreferences(prefs.copy(visualEnabled = it))
                        },
                    )
                    AlertToggle(
                        label = stringResource(R.string.alert_sound),
                        checked = prefs.soundEnabled,
                        onCheckedChange = {
                            viewModel.setAlertPreferences(prefs.copy(soundEnabled = it))
                        },
                    )
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
}

@Composable
private fun AlertToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
