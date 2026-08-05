package com.keepstraight.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WatchOff
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.keepstraight.R
import com.keepstraight.shared.presentation.ReconnectError
import com.keepstraight.shared.presentation.ReconnectUiState
import com.keepstraight.ui.components.KeepStraightTopBar
import com.keepstraight.ui.components.StatusPanel
import com.keepstraight.ui.components.StatusTone
import com.keepstraight.ui.connection.reconnectErrorBody
import com.keepstraight.presentation.connection.ConnectionConfig
import com.keepstraight.presentation.connection.ConnectionViewModel

@Composable
fun ConnectionFlowScreen(
    viewModel: ConnectionViewModel,
    autoStart: Boolean,
    onBack: () -> Unit,
    onChangeWatch: () -> Unit,
    onOpenBluetooth: () -> Unit,
) {
    val reconnectState by viewModel.reconnectState.collectAsState()
    val pairedWatchId by viewModel.pairedWatchId.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()

    LaunchedEffect(autoStart) {
        if (autoStart) {
            viewModel.reconnectWatch()
        }
    }

    LaunchedEffect(reconnectState) {
        if (reconnectState is ReconnectUiState.Success) {
            kotlinx.coroutines.delay(ConnectionConfig.SUCCESS_DISMISS_MS)
            if (viewModel.reconnectState.value is ReconnectUiState.Success) {
                viewModel.clearReconnectError()
                onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            KeepStraightTopBar(
                title = stringResource(R.string.connection_flow_title),
                onBack = {
                    viewModel.clearReconnectError()
                    onBack()
                },
            )
        },
    ) { padding ->
        val panelModifier = Modifier.padding(padding)
        when (val state = reconnectState) {
            ReconnectUiState.InProgress -> {
                StatusPanel(
                    tone = StatusTone.PROGRESS,
                    showProgress = true,
                    icon = Icons.AutoMirrored.Outlined.BluetoothSearching,
                    title = stringResource(R.string.connection_flow_progress_title),
                    body = stringResource(R.string.connection_flow_progress_body),
                    modifier = panelModifier,
                )
            }

            ReconnectUiState.Success -> {
                StatusPanel(
                    tone = StatusTone.SUCCESS,
                    icon = Icons.Outlined.CheckCircle,
                    title = stringResource(R.string.connection_flow_success_title),
                    body = stringResource(R.string.connection_flow_success_body),
                    primaryActionLabel = stringResource(R.string.connection_flow_done),
                    onPrimaryAction = {
                        viewModel.clearReconnectError()
                        onBack()
                    },
                    modifier = panelModifier,
                )
            }

            is ReconnectUiState.Failed -> {
                StatusPanel(
                    tone = StatusTone.ERROR,
                    icon = Icons.Outlined.ErrorOutline,
                    title = stringResource(R.string.connection_flow_failed_title),
                    body = reconnectErrorBody(state.reason),
                    primaryActionLabel = stringResource(R.string.dashboard_reconnect),
                    onPrimaryAction = viewModel::reconnectWatch,
                    secondaryActionLabel = when (state.reason) {
                        ReconnectError.NO_PAIRED_WATCH -> stringResource(R.string.settings_change_watch)
                        else -> stringResource(R.string.connection_flow_open_bluetooth)
                    },
                    onSecondaryAction = {
                        when (state.reason) {
                            ReconnectError.NO_PAIRED_WATCH -> onChangeWatch()
                            else -> onOpenBluetooth()
                        }
                    },
                    modifier = panelModifier,
                )
            }

            ReconnectUiState.Idle -> {
                if (isConnected) {
                    StatusPanel(
                        tone = StatusTone.SUCCESS,
                        icon = Icons.Outlined.CheckCircle,
                        title = stringResource(R.string.connection_flow_connected_title),
                        body = stringResource(R.string.connection_flow_connected_body),
                        primaryActionLabel = stringResource(R.string.connection_flow_done),
                        onPrimaryAction = onBack,
                        modifier = panelModifier,
                    )
                } else {
                    StatusPanel(
                        tone = StatusTone.WARNING,
                        icon = Icons.Outlined.WatchOff,
                        title = stringResource(R.string.connection_flow_disconnected_title),
                        body = if (pairedWatchId == null) {
                            stringResource(R.string.connection_flow_no_pair_body)
                        } else {
                            stringResource(R.string.connection_flow_disconnected_body)
                        },
                        primaryActionLabel = if (pairedWatchId == null) {
                            stringResource(R.string.settings_change_watch)
                        } else {
                            stringResource(R.string.dashboard_reconnect)
                        },
                        onPrimaryAction = {
                            if (pairedWatchId == null) onChangeWatch() else viewModel.reconnectWatch()
                        },
                        secondaryActionLabel = stringResource(R.string.connection_flow_open_bluetooth),
                        onSecondaryAction = onOpenBluetooth,
                        modifier = panelModifier,
                    )
                }
            }
        }
    }
}
