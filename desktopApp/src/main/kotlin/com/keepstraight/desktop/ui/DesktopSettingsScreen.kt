package com.keepstraight.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keepstraight.desktop.DesktopSessionController
import com.keepstraight.desktop.alert.NativeDesktopNotifier
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.presentation.BridgeConnectionState
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun DesktopSettingsScreen(
    controller: DesktopSessionController,
    onBack: () -> Unit,
    onHideToTray: (() -> Unit)?,
    onOpenCompanionSetup: () -> Unit,
    onQuit: () -> Unit,
) {
    val ui by controller.uiState.collectAsState()
    val lowPower by controller.lowPower.collectAsState()
    val openAtLogin by controller.openAtLogin.collectAsState()
    val sound by controller.desktopSoundEnabled.collectAsState()
    val notifications by controller.desktopNotificationEnabled.collectAsState()
    val startHidden by controller.startHiddenInTray.collectAsState()
    val bridgeHost by controller.bridgeHost.collectAsState()
    val bridgeState by controller.bridgeState.collectAsState()
    val bridgeMsg by controller.bridgeActionMessage.collectAsState()
    val bridgeBusy by controller.bridgeActionBusy.collectAsState()
    val qrBitmap by controller.pairQrBitmap.collectAsState()
    val qrActive by controller.qrPairingActive.collectAsState()
    val pairMessage by controller.pairMessage.collectAsState()
    val openAtLoginMessage by controller.openAtLoginMessage.collectAsState()

    DisposableEffect(Unit) {
        onDispose { controller.cancelPairQr() }
    }

    val devicesFlow = controller.devices ?: MutableStateFlow(emptyList())
    val selectedFlow = controller.selectedDeviceId ?: MutableStateFlow(null)
    val devices by devicesFlow.collectAsState()
    val selectedId by selectedFlow.collectAsState()

    DesktopPage {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineLarge)
            TextButton(onClick = onBack) { Text("Done") }
        }

        SettingsSection("Alerts") {
            SettingsToggle(
                "Play a sound when you slouch",
                "Off by default — it can be distracting.",
                sound,
                controller::setDesktopSoundEnabled,
            )
            SettingsToggle(
                "Show a desktop notification",
                if (NativeDesktopNotifier.isLikelySupported()) {
                    "Native OS banners — works while the app is in the tray."
                } else {
                    "Native notifications unavailable on this computer."
                },
                notifications,
                controller::setDesktopNotificationEnabled,
            )
            if (notifications) {
                OutlinedButton(
                    onClick = controller::sendTestDesktopNotification,
                    colors = desktopSecondaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text("Send test notification") }
            }
        }

        SettingsSection("Phone & watch") {
            Text(
                companionStatusText(bridgeState, bridgeHost),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(DesktopDimens.rowGap)) {
                Button(
                    onClick = onOpenCompanionSetup,
                    colors = desktopPrimaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text("Set up phone & watch") }
                if (bridgeState != BridgeConnectionState.NOT_CONFIGURED) {
                    OutlinedButton(
                        onClick = controller::reconnectBridge,
                        enabled = !bridgeBusy,
                        colors = desktopSecondaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) { Text(if (bridgeBusy) "Checking…" else "Reconnect") }
                }
            }
            if (qrActive && qrBitmap != null) {
                Image(
                    bitmap = qrBitmap!!,
                    contentDescription = "QR to link phone",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(DesktopDimens.radiusMedium)),
                )
                OutlinedButton(
                    onClick = controller::cancelPairQr,
                    colors = desktopSecondaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text("Cancel QR") }
            } else {
                OutlinedButton(
                    onClick = controller::showPairQr,
                    colors = desktopSecondaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text("Show QR to link phone") }
            }
            TextButton(
                onClick = controller::clearBridge,
                enabled = bridgeState != BridgeConnectionState.NOT_CONFIGURED,
            ) { Text("Unlink phone") }
            bridgeMsg?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            pairMessage?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        SettingsSection("Camera") {
            Text(
                devices.firstOrNull { it.id == selectedId }?.name
                    ?: devices.firstOrNull()?.name
                    ?: "No camera found",
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(DesktopDimens.rowGap)) {
                devices.forEach { device ->
                    val selected = device.id == selectedId
                    if (selected) {
                        Button(
                            onClick = { controller.selectCamera(device.id) },
                            colors = desktopPrimaryButtonColors(),
                            shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                        ) { Text(device.name, maxLines = 1) }
                    } else {
                        OutlinedButton(
                            onClick = { controller.selectCamera(device.id) },
                            colors = desktopSecondaryButtonColors(),
                            shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                        ) { Text(device.name, maxLines = 1) }
                    }
                }
                OutlinedButton(
                    onClick = controller::refreshCameras,
                    colors = desktopSecondaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text("Refresh") }
            }
        }

        SettingsSection(
            if (ui.settingsFromPhone) "Sensitivity (from phone)" else "Sensitivity",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(DesktopDimens.rowGap)) {
                SensitivityLevel.entries.forEach { level ->
                    val selected = ui.sensitivity == level
                    if (selected) {
                        Button(
                            onClick = { controller.setSensitivity(level) },
                            enabled = !ui.settingsFromPhone,
                            colors = desktopPrimaryButtonColors(),
                            shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                        ) { Text(level.name) }
                    } else {
                        OutlinedButton(
                            onClick = { controller.setSensitivity(level) },
                            enabled = !ui.settingsFromPhone,
                            colors = desktopSecondaryButtonColors(),
                            shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                        ) { Text(level.name) }
                    }
                }
            }
            Text(
                "First alert after ${formatMs(ui.slumpDurationThresholdMs)} · " +
                    "repeat every ${formatMs(ui.repeatAlertIntervalMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (ui.settingsFromPhone) {
                Text(
                    "Your phone owns these while it’s linked. Change them in the phone app, " +
                        "or unlink the phone to control them here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SettingsSection("General") {
            SettingsToggle("Low power (fewer FPS)", null, lowPower, controller::setLowPower)
            SettingsToggle(
                label = "Open at login",
                subtitle = openAtLoginMessage ?: if (controller.openAtLoginAvailable) {
                    "Start KeepStraight automatically when you sign in."
                } else {
                    "Unavailable — no packaged app or project launcher found."
                },
                checked = openAtLogin,
                onChange = controller::setOpenAtLogin,
                enabled = controller.openAtLoginAvailable,
            )
            SettingsToggle(
                label = "Start hidden in the menu bar / tray",
                subtitle = if (onHideToTray != null) {
                    "Next launch opens quietly in the background."
                } else {
                    "Needs a working menu bar / tray icon on this desktop."
                },
                checked = startHidden,
                onChange = controller::setStartHiddenInTray,
                enabled = onHideToTray != null,
            )
            onHideToTray?.let {
                OutlinedButton(
                    onClick = it,
                    modifier = Modifier.fillMaxWidth(),
                    colors = desktopSecondaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text("Hide window now") }
            }
        }

        Spacer(modifier = Modifier.height(DesktopDimens.itemGap))
        TextButton(onClick = onQuit) { Text("Quit KeepStraight") }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    DesktopCard {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

@Composable
private fun SettingsToggle(
    label: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = DesktopDimens.rowGap)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedThumbColor = MaterialTheme.colorScheme.surface,
            ),
        )
    }
}

private fun companionStatusText(state: BridgeConnectionState, host: String): String = when (state) {
    BridgeConnectionState.NOT_CONFIGURED -> "Phone not linked. Optional — desktop works on its own."
    BridgeConnectionState.PAIRED -> "Linked with $host"
    BridgeConnectionState.DEGRADED -> "Linked with $host, but sync is having trouble."
    BridgeConnectionState.FAILED -> "Link broken — unlink and scan a new QR."
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    return if (totalSec < 60L) "${totalSec}s" else "${totalSec / 60L}m"
}
