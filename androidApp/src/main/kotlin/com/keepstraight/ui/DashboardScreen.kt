package com.keepstraight.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keepstraight.R
import com.keepstraight.ui.components.BatteryOptimizationBanner
import com.keepstraight.ui.components.ConnectionBanner
import com.keepstraight.ui.components.KeepStraightTopBar
import com.keepstraight.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onRecalibrate: () -> Unit,
    onHistory: () -> Unit,
    onAlertSettings: () -> Unit,
    onSensitivity: () -> Unit,
    onSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val monitoringEnabled by viewModel.monitoringEnabled.collectAsState()
    val alertsEnabled by viewModel.alertsEnabled.collectAsState()
    val batteryDismissed by viewModel.batteryOptimizationDismissed.collectAsState()
    val showBatteryBanner by viewModel.showBatteryBanner.collectAsState()

    Scaffold(
        topBar = {
            KeepStraightTopBar(title = stringResource(R.string.dashboard_title))
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (showBatteryBanner && !batteryDismissed) {
                BatteryOptimizationBanner(
                    onOpenSettings = onOpenBatterySettings,
                    onDismiss = viewModel::dismissBatteryOptimizationBanner,
                )
            }

            if (!isConnected) {
                ConnectionBanner(onReconnect = viewModel::reconnectWatch)
            } else {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = stringResource(R.string.dashboard_connected),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.dashboard_connected_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRecalibrate,
                enabled = isConnected,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.dashboard_recalibrate),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.dashboard_recalibrate_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    ToggleRow(
                        label = stringResource(R.string.dashboard_monitoring),
                        subtitle = stringResource(R.string.dashboard_monitoring_subtitle),
                        checked = monitoringEnabled,
                        enabled = isConnected,
                        onCheckedChange = viewModel::setMonitoringEnabled,
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    ToggleRow(
                        label = stringResource(R.string.dashboard_alerts),
                        subtitle = stringResource(R.string.dashboard_alerts_subtitle),
                        checked = alertsEnabled,
                        enabled = isConnected,
                        onCheckedChange = viewModel::setAlertsEnabled,
                    )
                }
            }

            NavLink(
                label = stringResource(R.string.dashboard_history),
                icon = { Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null) },
                onClick = onHistory,
            )
            NavLink(
                label = stringResource(R.string.dashboard_alert_settings),
                icon = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
                onClick = onAlertSettings,
            )
            NavLink(
                label = stringResource(R.string.dashboard_sensitivity),
                icon = { Icon(Icons.Outlined.Build, contentDescription = null) },
                onClick = onSensitivity,
            )
            NavLink(
                label = stringResource(R.string.dashboard_settings),
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                onClick = onSettings,
            )
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (enabled) subtitle else stringResource(R.string.dashboard_requires_watch),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun NavLink(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Text(
                text = label,
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
