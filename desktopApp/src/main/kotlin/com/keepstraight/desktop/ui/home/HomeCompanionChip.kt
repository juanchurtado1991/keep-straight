package com.keepstraight.desktop.ui.home

import com.keepstraight.desktop.presentation.UserMessage
import com.keepstraight.desktop.ui.i18n.DesktopMessageResolver
import com.keepstraight.desktop.ui.i18n.DesktopStrings
import androidx.compose.runtime.Composable
import com.keepstraight.desktop.ui.DesktopInfoPanel
import com.keepstraight.shared.presentation.BridgeConnectionState

data class CompanionChipCopy(
    val title: String,
    val body: String,
    val primaryLabel: String,
    val onPrimary: () -> Unit,
    val secondaryLabel: String? = null,
    val onSecondary: (() -> Unit)? = null,
)

@Composable
fun HomeCompanionChip(
    bridgeState: BridgeConnectionState,
    bridgeHost: String,
    busy: Boolean,
    message: UserMessage?,
    onSetup: () -> Unit,
    onReconnect: () -> Unit,
) {
    val copy = companionChipCopy(
        bridgeState = bridgeState,
        bridgeHost = bridgeHost,
        busy = busy,
        onSetup = onSetup,
        onReconnect = onReconnect,
    )
    DesktopInfoPanel(
        title = copy.title,
        body = listOfNotNull(copy.body, DesktopMessageResolver.text(message)).joinToString("\n"),
        primaryLabel = copy.primaryLabel,
        onPrimary = copy.onPrimary,
        secondaryLabel = copy.secondaryLabel,
        onSecondary = copy.onSecondary,
    )
}

@Composable
private fun companionChipCopy(
    bridgeState: BridgeConnectionState,
    bridgeHost: String,
    busy: Boolean,
    onSetup: () -> Unit,
    onReconnect: () -> Unit,
): CompanionChipCopy = when (bridgeState) {
    BridgeConnectionState.NOT_CONFIGURED -> CompanionChipCopy(
        title = DesktopStrings.bridgeNotLinkedTitle(),
        body = DesktopStrings.bridgeNotLinkedBody(),
        primaryLabel = DesktopStrings.actionSetupPhoneWatch(),
        onPrimary = onSetup,
    )
    BridgeConnectionState.PAIRED -> CompanionChipCopy(
        title = DesktopStrings.bridgeLinkedTitle(),
        body = if (bridgeHost.isBlank()) {
            DesktopStrings.bridgeConnected()
        } else {
            DesktopStrings.bridgeConnectedTo(bridgeHost)
        },
        primaryLabel = DesktopStrings.actionManage(),
        onPrimary = onSetup,
        secondaryLabel = if (busy) {
            DesktopStrings.actionChecking()
        } else {
            DesktopStrings.actionReconnect()
        },
        onSecondary = onReconnect,
    )
    BridgeConnectionState.DEGRADED -> CompanionChipCopy(
        title = DesktopStrings.bridgeDegradedTitle(),
        body = DesktopStrings.bridgeDegradedBody(),
        primaryLabel = if (busy) {
            DesktopStrings.actionChecking()
        } else {
            DesktopStrings.actionReconnect()
        },
        onPrimary = onReconnect,
        secondaryLabel = DesktopStrings.actionSetupAgain(),
        onSecondary = onSetup,
    )
    BridgeConnectionState.FAILED -> CompanionChipCopy(
        title = DesktopStrings.bridgeFailedTitle(),
        body = DesktopStrings.bridgeFailedBody(),
        primaryLabel = DesktopStrings.actionScanNewQr(),
        onPrimary = onSetup,
    )
}
