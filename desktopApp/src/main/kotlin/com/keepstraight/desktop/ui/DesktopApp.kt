package com.keepstraight.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keepstraight.desktop.DesktopSessionController
import com.keepstraight.desktop.ui.wizard.CompanionSetupFlow
import com.keepstraight.shared.domain.DesktopSessionPhase
import com.keepstraight.shared.presentation.BridgeConnectionState
import com.keepstraight.shared.presentation.DesktopStatusAction
import com.keepstraight.shared.presentation.DesktopStatusPresentation
import com.keepstraight.shared.presentation.DesktopStatusTone

private enum class DesktopRoute {
    Home,
    Calibrate,
    Settings,
    CompanionSetup,
}

@Composable
fun DesktopApp(
    controller: DesktopSessionController,
    onQuit: () -> Unit,
    onHideToTray: (() -> Unit)?,
) {
    var route by remember { mutableStateOf(DesktopRoute.Home) }

    LaunchedEffect(route) { controller.clearBridgeActionMessage() }

    when (route) {
        DesktopRoute.Calibrate -> CalibrationScreen(
            controller = controller,
            onClose = { route = DesktopRoute.Home },
        )
        DesktopRoute.Settings -> DesktopSettingsScreen(
            controller = controller,
            onBack = { route = DesktopRoute.Home },
            onHideToTray = onHideToTray,
            onOpenCompanionSetup = { route = DesktopRoute.CompanionSetup },
            onQuit = onQuit,
        )
        DesktopRoute.CompanionSetup -> CompanionSetupFlow(
            controller = controller,
            onFinished = { route = DesktopRoute.Home },
        )
        DesktopRoute.Home -> HomeScreen(
            controller = controller,
            onOpenCalibrate = { route = DesktopRoute.Calibrate },
            onOpenSettings = { route = DesktopRoute.Settings },
            onOpenCompanion = { route = DesktopRoute.CompanionSetup },
            onHideToTray = onHideToTray,
        )
    }
}

@Composable
private fun HomeScreen(
    controller: DesktopSessionController,
    onOpenCalibrate: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCompanion: () -> Unit,
    onHideToTray: (() -> Unit)?,
) {
    val ui by controller.uiState.collectAsState()
    val status by controller.statusPresentation.collectAsState()
    val bridgeState by controller.bridgeState.collectAsState()
    val bridgeHost by controller.bridgeHost.collectAsState()
    val bridgeMsg by controller.bridgeActionMessage.collectAsState()
    val bridgeBusy by controller.bridgeActionBusy.collectAsState()

    DesktopPage {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(DesktopDimens.itemGap)) {
                Text("KeepStraight", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Desktop posture companion",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        DesktopStatusCard(
            status = status,
            slumpPercent = (ui.slumpScore * 100f).toInt().coerceIn(0, 150),
            onAction = { action ->
                when (action) {
                    DesktopStatusAction.CALIBRATE,
                    DesktopStatusAction.CALIBRATE_ERECT,
                    DesktopStatusAction.CALIBRATE_SLUMP,
                    -> onOpenCalibrate()
                    DesktopStatusAction.REPAIR_BRIDGE -> onOpenCompanion()
                    else -> controller.handleStatusAction(action)
                }
            },
        )

        DesktopCard {
            Row(horizontalArrangement = Arrangement.spacedBy(DesktopDimens.rowGap)) {
                if (ui.phase == DesktopSessionPhase.IDLE) {
                    Button(
                        onClick = controller::startSession,
                        enabled = ui.modelReady && ui.hasCalibration,
                        colors = desktopPrimaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) { Text("Start") }
                } else {
                    OutlinedButton(
                        onClick = controller::stopSession,
                        colors = desktopSecondaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) { Text("Stop") }
                }
                Button(
                    onClick = onOpenCalibrate,
                    enabled = ui.modelReady,
                    colors = desktopPrimaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text("Calibrate") }
                onHideToTray?.let {
                    OutlinedButton(
                        onClick = it,
                        colors = desktopSecondaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) { Text("Hide to tray") }
                }
            }
        }

        CompanionChip(
            bridgeState = bridgeState,
            bridgeHost = bridgeHost,
            busy = bridgeBusy,
            message = bridgeMsg,
            onSetup = onOpenCompanion,
            onReconnect = controller::reconnectBridge,
        )

        Text(
            "Frames are not saved. Phone and watch are optional.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private class CompanionChipCopy(
    val title: String,
    val body: String,
    val primaryLabel: String,
    val onPrimary: () -> Unit,
    val secondaryLabel: String? = null,
    val onSecondary: (() -> Unit)? = null,
)

@Composable
private fun CompanionChip(
    bridgeState: BridgeConnectionState,
    bridgeHost: String,
    busy: Boolean,
    message: String?,
    onSetup: () -> Unit,
    onReconnect: () -> Unit,
) {
    val copy = when (bridgeState) {
        BridgeConnectionState.NOT_CONFIGURED -> CompanionChipCopy(
            title = "Phone not linked",
            body = "Optional — add phone & watch when you want wrist alerts.",
            primaryLabel = "Set up phone & watch",
            onPrimary = onSetup,
        )
        BridgeConnectionState.PAIRED -> CompanionChipCopy(
            title = "Phone linked",
            body = if (bridgeHost.isBlank()) "Connected" else "Connected to $bridgeHost",
            primaryLabel = "Manage",
            onPrimary = onSetup,
            secondaryLabel = if (busy) "Checking…" else "Reconnect",
            onSecondary = onReconnect,
        )
        BridgeConnectionState.DEGRADED -> CompanionChipCopy(
            title = "Phone link needs attention",
            body = "We can’t sync right now. Reconnect, or set it up again.",
            primaryLabel = if (busy) "Checking…" else "Reconnect",
            onPrimary = onReconnect,
            secondaryLabel = "Set up again",
            onSecondary = onSetup,
        )
        BridgeConnectionState.FAILED -> CompanionChipCopy(
            title = "Phone link broken",
            body = "The phone no longer trusts this computer. Scan a new QR to link again.",
            primaryLabel = "Scan a new QR",
            onPrimary = onSetup,
        )
    }
    DesktopInfoPanel(
        title = copy.title,
        body = listOfNotNull(copy.body, message).joinToString("\n"),
        primaryLabel = copy.primaryLabel,
        onPrimary = copy.onPrimary,
        secondaryLabel = copy.secondaryLabel,
        onSecondary = copy.onSecondary,
    )
}

@Composable
private fun DesktopStatusCard(
    status: DesktopStatusPresentation,
    slumpPercent: Int,
    onAction: (DesktopStatusAction) -> Unit,
) {
    val accent = toneAccent(status.tone)
    DesktopCard(containerColor = toneContainer(status.tone)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Spacer(modifier = Modifier.width(DesktopDimens.rowGap))
            Text(
                status.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (status.showProgress) {
                Spacer(modifier = Modifier.width(DesktopDimens.rowGap))
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = accent,
                )
            }
        }
        Text(status.body, style = MaterialTheme.typography.bodyLarge)
        status.presenceLabel?.let {
            Text(
                "Presence: $it · Slump score: $slumpPercent%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } ?: Text(
            "Slump score: $slumpPercent%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(DesktopDimens.rowGap)) {
            status.primaryAction?.let { action ->
                Button(
                    onClick = { onAction(action) },
                    colors = desktopPrimaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text(actionLabel(action)) }
            }
            status.secondaryAction?.let { action ->
                OutlinedButton(
                    onClick = { onAction(action) },
                    colors = desktopSecondaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text(actionLabel(action)) }
            }
        }
    }
}

private fun actionLabel(action: DesktopStatusAction): String = when (action) {
    DesktopStatusAction.RETRY_CAMERA -> "Retry camera"
    DesktopStatusAction.REFRESH_CAMERAS -> "Refresh cameras"
    DesktopStatusAction.CALIBRATE,
    DesktopStatusAction.CALIBRATE_ERECT,
    DesktopStatusAction.CALIBRATE_SLUMP,
    -> "Calibrate"
    DesktopStatusAction.STOP_SESSION -> "Stop"
    DesktopStatusAction.CLEAR_BRIDGE -> "Unlink phone"
    DesktopStatusAction.REPAIR_BRIDGE -> "Fix phone link"
}

@Composable
private fun toneAccent(tone: DesktopStatusTone): Color = when (tone) {
    DesktopStatusTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    DesktopStatusTone.PROGRESS -> MaterialTheme.colorScheme.primary
    DesktopStatusTone.SUCCESS -> StatusGood
    DesktopStatusTone.WARNING -> StatusWarning
    DesktopStatusTone.ERROR -> MaterialTheme.colorScheme.error
}

@Composable
private fun toneContainer(tone: DesktopStatusTone): Color = when (tone) {
    DesktopStatusTone.NEUTRAL -> MaterialTheme.colorScheme.surface
    DesktopStatusTone.PROGRESS -> MaterialTheme.colorScheme.primaryContainer
    DesktopStatusTone.SUCCESS -> StatusGoodContainer
    DesktopStatusTone.WARNING -> StatusWarningContainer
    DesktopStatusTone.ERROR -> MaterialTheme.colorScheme.errorContainer
}
