package com.keepstraight.desktop.ui.wizard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keepstraight.desktop.DesktopSessionController
import com.keepstraight.desktop.ui.DesktopCard
import com.keepstraight.desktop.ui.DesktopDimens
import com.keepstraight.desktop.ui.DesktopErrorPanel
import com.keepstraight.desktop.ui.DesktopInfoPanel
import com.keepstraight.desktop.ui.DesktopPage
import com.keepstraight.desktop.ui.desktopPrimaryButtonColors
import com.keepstraight.desktop.ui.desktopSecondaryButtonColors
import com.keepstraight.shared.presentation.BridgeConnectionState

enum class CompanionSetupStep {
    Hub,
    InstallPhone,
    LinkPhone,
    InstallWatch,
}

/**
 * Phone install, phone↔desktop link, and watch install are independent optional steps.
 */
@Composable
fun CompanionSetupFlow(
    controller: DesktopSessionController,
    onFinished: () -> Unit,
    startAt: CompanionSetupStep = CompanionSetupStep.Hub,
) {
    var step by remember { mutableStateOf(startAt) }
    val bridgeState by controller.bridgeState.collectAsState()

    when (step) {
        CompanionSetupStep.Hub -> DesktopPage {
            Text("Phone & watch", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Optional add-ons — pick any step, or skip them all. Suggested order: watch first, then phone, then link.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DesktopInfoPanel(
                title = when (bridgeState) {
                    BridgeConnectionState.PAIRED -> "Phone link: connected"
                    BridgeConnectionState.DEGRADED -> "Phone link: needs reconnect"
                    BridgeConnectionState.FAILED -> "Phone link: expired — scan a new QR"
                    BridgeConnectionState.NOT_CONFIGURED -> "Phone link: not set up"
                },
                body = when (bridgeState) {
                    BridgeConnectionState.PAIRED ->
                        "History and watch alerts go through the phone. You can still reinstall apps below."
                    BridgeConnectionState.NOT_CONFIGURED ->
                        "Install the watch app before the phone so the phone can see it. Linking is only needed for history and wrist alerts — desktop works alone."
                    else ->
                        "Use “Link phone” for a fresh QR, or reconnect from Home if the token is still valid."
                },
            )
            DesktopCard {
                Button(
                    onClick = { step = CompanionSetupStep.InstallWatch },
                    modifier = Modifier.fillMaxWidth(),
                    colors = desktopPrimaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text("1. Install watch app") }
                OutlinedButton(
                    onClick = { step = CompanionSetupStep.InstallPhone },
                    modifier = Modifier.fillMaxWidth(),
                    colors = desktopSecondaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text("2. Install phone app") }
                OutlinedButton(
                    onClick = { step = CompanionSetupStep.LinkPhone },
                    modifier = Modifier.fillMaxWidth(),
                    colors = desktopSecondaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text("3. Link phone to desktop") }
                Button(
                    onClick = onFinished,
                    modifier = Modifier.fillMaxWidth(),
                    colors = desktopPrimaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text("Done") }
            }
        }
        CompanionSetupStep.InstallPhone -> WirelessPhoneInstallStep(
            onInstalled = { step = CompanionSetupStep.Hub },
            onSkip = { step = CompanionSetupStep.Hub },
        )
        CompanionSetupStep.LinkPhone -> LinkPhoneQrStep(
            controller = controller,
            onContinue = { step = CompanionSetupStep.Hub },
            onSkip = { step = CompanionSetupStep.Hub },
        )
        CompanionSetupStep.InstallWatch -> WirelessWatchInstallStep(
            onInstalled = { step = CompanionSetupStep.Hub },
            onSkip = { step = CompanionSetupStep.Hub },
        )
    }
}

@Composable
private fun LinkPhoneQrStep(
    controller: DesktopSessionController,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    val bridgeState by controller.bridgeState.collectAsState()
    val qrBitmap by controller.pairQrBitmap.collectAsState()
    val qrActive by controller.qrPairingActive.collectAsState()
    val pairMessage by controller.pairMessage.collectAsState()
    val bridgeMsg by controller.bridgeActionMessage.collectAsState()
    val startedPaired = remember {
        controller.bridgeState.value == BridgeConnectionState.PAIRED
    }

    DisposableEffect(Unit) {
        controller.showPairQr()
        onDispose { controller.cancelPairQr() }
    }

    LaunchedEffect(bridgeState) {
        if (!startedPaired && bridgeState == BridgeConnectionState.PAIRED) {
            onContinue()
        }
    }

    DesktopPage {
        Text("Link phone to desktop", style = MaterialTheme.typography.headlineLarge)
        Text(
            "This is only the KeepStraight link (not ADB). Open the phone app → scan this code. Same Wi‑Fi.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DesktopInfoPanel(
            title = "Need the phone app first?",
            body = "If KeepStraight isn’t on the phone yet, go back and choose Install phone app. Linking doesn’t install anything.",
        )
        DesktopCard {
            if (qrActive && qrBitmap != null) {
                Image(
                    bitmap = qrBitmap!!,
                    contentDescription = "QR to link KeepStraight phone",
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(DesktopDimens.radiusMedium)),
                )
                Text("Waiting for your phone…", fontWeight = FontWeight.Medium)
            } else {
                DesktopErrorPanel(
                    title = "Couldn’t show a QR",
                    body = "Try again. Check that nothing else is using the pairing port.",
                    primaryLabel = "Try again",
                    onPrimary = controller::showPairQr,
                    secondaryLabel = "Back",
                    onSecondary = onSkip,
                )
            }
            pairMessage?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            bridgeMsg?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DesktopDimens.rowGap)) {
                OutlinedButton(
                    onClick = {
                        controller.cancelPairQr()
                        controller.showPairQr()
                    },
                    colors = desktopSecondaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text("New QR") }
                if (bridgeState == BridgeConnectionState.PAIRED) {
                    Button(
                        onClick = onContinue,
                        colors = desktopPrimaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) { Text("Done linking") }
                }
                TextButton(onClick = onSkip) { Text("Back") }
            }
        }
    }
}
