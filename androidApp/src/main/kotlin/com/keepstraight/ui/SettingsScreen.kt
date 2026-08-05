package com.keepstraight.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.keepstraight.KeepStraightApp
import com.keepstraight.R
import com.keepstraight.presentation.settings.SettingsViewModel
import com.keepstraight.ui.components.KeepStraightTopBar
import com.keepstraight.ui.settings.SettingsLink
import com.keepstraight.ui.settings.SettingsToggleRow
import com.keepstraight.ui.settings.watchToggleSubtitle
import com.keepstraight.ui.theme.PhoneCard
import com.keepstraight.ui.theme.PhonePage
import com.keepstraight.ui.theme.phoneButtonShape
import com.keepstraight.ui.theme.phoneSecondaryButtonColors

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onHistory: () -> Unit,
    onAlertSettings: () -> Unit,
    onSensitivity: () -> Unit,
    onChangePairedWatch: () -> Unit,
    onScanDesktopQr: () -> Unit,
    onOpenConnection: () -> Unit,
    onBack: () -> Unit,
) {
    val pairedWatchId by viewModel.pairedWatchId.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val monitoringEnabled by viewModel.monitoringEnabled.collectAsState()
    val alertsEnabled by viewModel.alertsEnabled.collectAsState()
    val app = LocalContext.current.applicationContext as KeepStraightApp
    val desktopPaired by app.lanIngestServer.desktopPaired.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            KeepStraightTopBar(
                title = stringResource(R.string.settings_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        PhonePage(modifier = Modifier.padding(padding)) {
            PhoneCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    text = stringResource(R.string.settings_paired_watch),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = pairedWatchId ?: stringResource(R.string.settings_no_watch),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            val watchPaired = pairedWatchId != null
            if (watchPaired) {
                PhoneCard {
                    SettingsLink(
                        label = stringResource(R.string.connection_flow_title),
                        onClick = onOpenConnection,
                    )
                }
            }
            PhoneCard {
                SettingsToggleRow(
                    label = stringResource(R.string.dashboard_monitoring),
                    subtitle = watchToggleSubtitle(
                        watchPaired = watchPaired,
                        isConnected = isConnected,
                        connectedSubtitle = R.string.dashboard_monitoring_subtitle,
                    ),
                    checked = monitoringEnabled,
                    enabled = watchPaired,
                    onCheckedChange = viewModel::setMonitoringEnabled,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsToggleRow(
                    label = stringResource(R.string.dashboard_alerts),
                    subtitle = watchToggleSubtitle(
                        watchPaired = watchPaired,
                        isConnected = isConnected,
                        connectedSubtitle = R.string.dashboard_alerts_subtitle,
                    ),
                    checked = alertsEnabled,
                    enabled = watchPaired,
                    onCheckedChange = viewModel::setAlertsEnabled,
                )
            }

            PhoneCard {
                Text(
                    text = stringResource(R.string.settings_desktop_bridge),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.settings_desktop_bridge_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (desktopPaired) {
                        stringResource(R.string.settings_desktop_bridge_paired)
                    } else {
                        stringResource(R.string.settings_desktop_bridge_not_paired)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedButton(
                    onClick = onScanDesktopQr,
                    modifier = Modifier.fillMaxWidth(),
                    shape = phoneButtonShape(),
                    colors = phoneSecondaryButtonColors(),
                ) {
                    Text(stringResource(R.string.settings_desktop_bridge_scan_qr))
                }
                OutlinedButton(
                    onClick = { app.lanIngestServer.clearPairing() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = phoneButtonShape(),
                    colors = phoneSecondaryButtonColors(),
                    enabled = desktopPaired,
                ) {
                    Text(stringResource(R.string.settings_desktop_bridge_clear))
                }
            }

            PhoneCard {
                SettingsLink(stringResource(R.string.dashboard_history), onHistory)
                SettingsLink(stringResource(R.string.dashboard_alert_settings), onAlertSettings)
                SettingsLink(stringResource(R.string.settings_sensitivity), onSensitivity)
                SettingsLink(stringResource(R.string.settings_notifications), onOpenNotificationSettings)
                SettingsLink(stringResource(R.string.settings_battery), onOpenBatterySettings)
                SettingsLink(stringResource(R.string.settings_bluetooth), onOpenBluetoothSettings)
                SettingsLink(stringResource(R.string.settings_app_details), onOpenAppDetails)
            }

            PhoneCard {
                Text(
                    text = stringResource(R.string.settings_recalibrate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.dashboard_recalibrate_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingsLink(stringResource(R.string.settings_change_watch), onChangePairedWatch)
            }

            OutlinedButton(
                onClick = viewModel::unpairWatch,
                modifier = Modifier.fillMaxWidth(),
                enabled = pairedWatchId != null,
                shape = phoneButtonShape(),
                colors = phoneSecondaryButtonColors(),
            ) {
                Text(stringResource(R.string.settings_unpair))
            }
        }
    }
}
