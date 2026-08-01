package com.keepstraight.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.keepstraight.R
import com.keepstraight.ui.components.KeepStraightTopBar
import com.keepstraight.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onOpenNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onBack: () -> Unit,
) {
    val pairedWatchId by viewModel.pairedWatchId.collectAsState()

    Scaffold(
        topBar = {
            KeepStraightTopBar(
                title = stringResource(R.string.settings_title),
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_paired_watch),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = pairedWatchId ?: stringResource(R.string.settings_no_watch),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            SettingsLink(stringResource(R.string.settings_notifications), onOpenNotificationSettings)
            SettingsLink(stringResource(R.string.settings_battery), onOpenBatterySettings)
            SettingsLink(stringResource(R.string.settings_bluetooth), onOpenBluetoothSettings)
            SettingsLink(stringResource(R.string.settings_app_details), onOpenAppDetails)

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            OutlinedButton(
                onClick = viewModel::unpairWatch,
                modifier = Modifier.fillMaxWidth(),
                enabled = pairedWatchId != null,
            ) {
                Text(stringResource(R.string.settings_unpair))
            }
        }
    }
}

@Composable
private fun SettingsLink(label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
