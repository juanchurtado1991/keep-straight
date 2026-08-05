package com.keepstraight.desktop.ui.home

import com.keepstraight.desktop.presentation.UserMessage
import com.keepstraight.desktop.ui.i18n.DesktopMessageResolver
import com.keepstraight.desktop.ui.i18n.DesktopStrings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.keepstraight.desktop.DesktopSessionController
import com.keepstraight.desktop.ui.DesktopCard
import com.keepstraight.desktop.ui.DesktopPage
import com.keepstraight.desktop.ui.desktopPrimaryButtonColors
import com.keepstraight.desktop.ui.desktopSecondaryButtonColors
import com.keepstraight.desktop.ui.theme.DesktopDimens
import com.keepstraight.shared.domain.DesktopSessionPhase
import com.keepstraight.shared.presentation.BridgeConnectionState
import com.keepstraight.shared.presentation.DesktopStatusAction
import com.keepstraight.shared.presentation.DesktopStatusPresentation
import com.keepstraight.shared.domain.DesktopSessionUiState

@Composable
fun HomeScreen(
    controller: DesktopSessionController,
    status: DesktopStatusPresentation,
    ui: DesktopSessionUiState,
    bridgeState: BridgeConnectionState,
    bridgeHost: String,
    bridgeMsg: UserMessage?,
    bridgeBusy: Boolean,
    onOpenCalibrate: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCompanion: () -> Unit,
    onHideToTray: (() -> Unit)?,
    onStatusAction: (DesktopStatusAction) -> Unit,
) {
    DesktopPage {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(DesktopDimens.itemGap)) {
                Text(DesktopStrings.appName(), style = MaterialTheme.typography.headlineLarge)
                Text(
                    DesktopStrings.appTagline(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = DesktopStrings.settingsTitle(),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        HomeStatusCard(
            status = status,
            slumpPercent = (ui.slumpScore * 100f).toInt().coerceIn(0, 150),
            onAction = onStatusAction,
        )

        DesktopCard {
            Row(horizontalArrangement = Arrangement.spacedBy(DesktopDimens.rowGap)) {
                if (ui.phase == DesktopSessionPhase.IDLE) {
                    Button(
                        onClick = controller::startSession,
                        enabled = ui.modelReady && ui.hasCalibration,
                        colors = desktopPrimaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) { Text(DesktopStrings.actionStart()) }
                } else {
                    OutlinedButton(
                        onClick = controller::stopSession,
                        colors = desktopSecondaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) { Text(DesktopStrings.actionStop()) }
                }
                Button(
                    onClick = onOpenCalibrate,
                    enabled = ui.modelReady,
                    colors = desktopPrimaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text(DesktopStrings.actionCalibrate()) }
                onHideToTray?.let {
                    OutlinedButton(
                        onClick = it,
                        colors = desktopSecondaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) { Text(DesktopStrings.actionHideToTray()) }
                }
            }
        }

        HomeCompanionChip(
            bridgeState = bridgeState,
            bridgeHost = bridgeHost,
            busy = bridgeBusy,
            message = bridgeMsg,
            onSetup = onOpenCompanion,
            onReconnect = controller::reconnectBridge,
        )

        Text(
            DesktopStrings.homePrivacyNote(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
