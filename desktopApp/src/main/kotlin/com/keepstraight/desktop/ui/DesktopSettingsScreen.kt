package com.keepstraight.desktop.ui

import com.keepstraight.desktop.presentation.UserMessage
import com.keepstraight.desktop.ui.i18n.DesktopMessageResolver
import com.keepstraight.desktop.ui.i18n.DesktopStrings
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
import com.keepstraight.sharedui.i18n.SharedStrings
import com.keepstraight.sharedui.sensitivity.SharedInlineSensitivitySection
import com.keepstraight.shared.presentation.BridgeConnectionState
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun DesktopSettingsScreen(
    controller: DesktopSessionController,
    onBack: () -> Unit,
    onHideToTray: (() -> Unit)?,
    onOpenCompanionSetup: () -> Unit,
    onOpenFullSensitivity: (() -> Unit)? = null,
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
            Text(DesktopStrings.settingsTitle(), style = MaterialTheme.typography.headlineLarge)
            TextButton(onClick = onBack) { Text(DesktopStrings.actionDone()) }
        }

        SettingsSection(DesktopStrings.settingsSectionAlerts()) {
            SettingsToggle(
                DesktopStrings.settingsSoundLabel(),
                DesktopStrings.settingsSoundSubtitle(),
                sound,
                controller::setDesktopSoundEnabled,
            )
            SettingsToggle(
                DesktopStrings.settingsNotificationLabel(),
                if (NativeDesktopNotifier.isLikelySupported()) {
                    DesktopStrings.settingsNotificationSubtitleSupported()
                } else {
                    DesktopStrings.settingsNotificationSubtitleUnsupported()
                },
                notifications,
                controller::setDesktopNotificationEnabled,
            )
            if (notifications) {
                OutlinedButton(
                    onClick = controller::sendTestDesktopNotification,
                    colors = desktopSecondaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text(DesktopStrings.actionSendTestNotification()) }
            }
        }

        SettingsSection(DesktopStrings.settingsSectionPhoneWatch()) {
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
                ) { Text(DesktopStrings.actionSetupPhoneWatch()) }
                if (bridgeState != BridgeConnectionState.NOT_CONFIGURED) {
                    OutlinedButton(
                        onClick = controller::reconnectBridge,
                        enabled = !bridgeBusy,
                        colors = desktopSecondaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) {
                        Text(
                            if (bridgeBusy) {
                                DesktopStrings.actionChecking()
                            } else {
                                DesktopStrings.actionReconnect()
                            },
                        )
                    }
                }
            }
            if (qrActive && qrBitmap != null) {
                Image(
                    bitmap = qrBitmap!!,
                    contentDescription = DesktopStrings.settingsQrLinkCd(),
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(DesktopDimens.radiusMedium)),
                )
                OutlinedButton(
                    onClick = controller::cancelPairQr,
                    colors = desktopSecondaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text(DesktopStrings.actionCancelQr()) }
            } else {
                OutlinedButton(
                    onClick = controller::showPairQr,
                    colors = desktopSecondaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text(DesktopStrings.actionShowQrLinkPhone()) }
            }
            TextButton(
                onClick = controller::clearBridge,
                enabled = bridgeState != BridgeConnectionState.NOT_CONFIGURED,
            ) { Text(DesktopStrings.actionUnlinkPhone()) }
            bridgeMsg?.let {
                Text(
                    DesktopMessageResolver.text(it).orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            pairMessage?.let {
                Text(
                    DesktopMessageResolver.text(it).orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SettingsSection(DesktopStrings.settingsSectionCamera()) {
            Text(
                devices.firstOrNull { it.id == selectedId }?.name?.let { DesktopStrings.cameraDisplayName(it) }
                    ?: devices.firstOrNull()?.name?.let { DesktopStrings.cameraDisplayName(it) }
                    ?: DesktopStrings.settingsNoCamera(),
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
                        ) { Text(DesktopStrings.cameraDisplayName(device.name), maxLines = 1) }
                    } else {
                        OutlinedButton(
                            onClick = { controller.selectCamera(device.id) },
                            colors = desktopSecondaryButtonColors(),
                            shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                        ) { Text(DesktopStrings.cameraDisplayName(device.name), maxLines = 1) }
                    }
                }
                OutlinedButton(
                    onClick = controller::refreshCameras,
                    colors = desktopSecondaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text(DesktopStrings.actionRefresh()) }
            }
        }

        SettingsSection(
            if (ui.settingsFromPhone) {
                SharedStrings.settingsSectionSensitivityFromPhone()
            } else {
                SharedStrings.settingsSectionSensitivity()
            },
        ) {
            SharedInlineSensitivitySection(
                sensitivity = ui.sensitivity,
                slumpDurationMs = ui.slumpDurationThresholdMs,
                repeatAlertMs = ui.repeatAlertIntervalMs,
                settingsFromPhone = ui.settingsFromPhone,
                sensitivityEnabled = !ui.settingsFromPhone,
                onSensitivityChange = controller::setSensitivity,
                sectionTitle = {},
            )
            if (onOpenFullSensitivity != null && !ui.settingsFromPhone) {
                TextButton(onClick = onOpenFullSensitivity) {
                    Text(SharedStrings.sensitivityTitle())
                }
            }
        }

        SettingsSection(DesktopStrings.settingsSectionGeneral()) {
            SettingsToggle(
                DesktopStrings.settingsLowPowerLabel(),
                null,
                lowPower,
                controller::setLowPower,
            )
            SettingsToggle(
                label = DesktopStrings.settingsOpenAtLoginLabel(),
                subtitle = DesktopMessageResolver.text(openAtLoginMessage)
                    ?: if (controller.openAtLoginAvailable) {
                        DesktopStrings.settingsOpenAtLoginSubtitle()
                    } else {
                        DesktopStrings.settingsOpenAtLoginUnavailable()
                    },
                checked = openAtLogin,
                onChange = controller::setOpenAtLogin,
                enabled = controller.openAtLoginAvailable,
            )
            SettingsToggle(
                label = DesktopStrings.settingsStartHiddenLabel(),
                subtitle = if (onHideToTray != null) {
                    DesktopStrings.settingsStartHiddenSubtitle()
                } else {
                    DesktopStrings.settingsStartHiddenUnavailable()
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
                ) { Text(DesktopStrings.actionHideWindowNow()) }
            }
        }

        Spacer(modifier = Modifier.height(DesktopDimens.itemGap))
        TextButton(onClick = onQuit) { Text(DesktopStrings.actionQuitApp()) }
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

@Composable
private fun companionStatusText(state: BridgeConnectionState, host: String): String = when (state) {
    BridgeConnectionState.NOT_CONFIGURED -> DesktopStrings.settingsBridgeNotLinked()
    BridgeConnectionState.PAIRED -> DesktopStrings.settingsBridgeLinked(host)
    BridgeConnectionState.DEGRADED -> DesktopStrings.settingsBridgeDegraded(host)
    BridgeConnectionState.FAILED -> DesktopStrings.settingsBridgeFailed()
}
