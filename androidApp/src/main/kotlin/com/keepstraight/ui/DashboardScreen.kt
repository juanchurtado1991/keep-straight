package com.keepstraight.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.keepstraight.KeepStraightApp
import com.keepstraight.R
import com.keepstraight.ui.components.BatteryOptimizationBanner
import com.keepstraight.ui.components.KeepStraightTopBar
import com.keepstraight.ui.components.WorkDayChartCard
import com.keepstraight.ui.theme.PhoneCard
import com.keepstraight.ui.theme.PhoneDimens
import com.keepstraight.ui.theme.StatusGoodContainer
import com.keepstraight.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onSettings: () -> Unit,
    onScanDesktopQr: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onFixConnection: () -> Unit,
    onConnectionStatus: () -> Unit,
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val pairedWatchId by viewModel.pairedWatchId.collectAsState()
    val batteryDismissed by viewModel.batteryOptimizationDismissed.collectAsState()
    val showBatteryBanner by viewModel.showBatteryBanner.collectAsState()
    val dayStats by viewModel.dashboardDays.collectAsState()
    val app = LocalContext.current.applicationContext as KeepStraightApp
    val desktopPaired by app.lanIngestServer.desktopPaired.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            KeepStraightTopBar(
                title = stringResource(R.string.dashboard_title),
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.dashboard_settings),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(PhoneDimens.pagePadding),
            verticalArrangement = Arrangement.spacedBy(PhoneDimens.sectionGap),
        ) {
            if (showBatteryBanner && !batteryDismissed) {
                item {
                    BatteryOptimizationBanner(
                        onOpenSettings = onOpenBatterySettings,
                        onDismiss = viewModel::dismissBatteryOptimizationBanner,
                    )
                }
            }

            item {
                when {
                    pairedWatchId == null -> StatusCard(
                        icon = Icons.Outlined.Watch,
                        title = stringResource(R.string.dashboard_watch_optional),
                        body = stringResource(R.string.dashboard_watch_optional_subtitle),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = onFixConnection,
                    )
                    isConnected -> StatusCard(
                        icon = Icons.Outlined.Watch,
                        title = stringResource(R.string.dashboard_connected),
                        body = stringResource(R.string.dashboard_connected_subtitle),
                        containerColor = StatusGoodContainer,
                        onClick = onConnectionStatus,
                    )
                    else -> StatusCard(
                        icon = Icons.Outlined.Watch,
                        title = stringResource(R.string.dashboard_disconnected),
                        body = stringResource(R.string.banner_connection_body),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        onClick = onFixConnection,
                    )
                }
            }

            item {
                StatusCard(
                    icon = Icons.Outlined.Computer,
                    title = stringResource(R.string.dashboard_desktop_card_title),
                    body = if (desktopPaired) {
                        stringResource(R.string.dashboard_desktop_card_paired)
                    } else {
                        stringResource(R.string.dashboard_desktop_card_body)
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    onClick = onScanDesktopQr,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(PhoneDimens.itemGap)) {
                    Text(
                        text = stringResource(R.string.dashboard_charts_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.dashboard_charts_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (dayStats.isEmpty()) {
                item {
                    PhoneCard {
                        Text(
                            text = stringResource(R.string.dashboard_charts_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(dayStats, key = { it.day.toString() }) { day ->
                    PhoneCard(
                        containerColor = if (day.inProgress) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ) {
                        WorkDayChartCard(day = day)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    icon: ImageVector,
    title: String,
    body: String,
    containerColor: Color,
    onClick: () -> Unit,
) {
    PhoneCard(
        modifier = Modifier.clickable(onClick = onClick),
        containerColor = containerColor,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
