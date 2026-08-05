package com.keepstraight.desktop.ui.wizard

import com.keepstraight.desktop.presentation.UserMessage
import com.keepstraight.desktop.ui.i18n.DesktopMessageResolver
import com.keepstraight.desktop.ui.i18n.DesktopStrings
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
            Text(DesktopStrings.wizardPhoneWatchTitle(), style = MaterialTheme.typography.headlineLarge)
            Text(
                DesktopStrings.wizardHubIntro(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DesktopInfoPanel(
                title = when (bridgeState) {
                    BridgeConnectionState.PAIRED -> DesktopStrings.wizardHubLinkPaired()
                    BridgeConnectionState.DEGRADED -> DesktopStrings.wizardHubLinkDegraded()
                    BridgeConnectionState.FAILED -> DesktopStrings.wizardHubLinkFailed()
                    BridgeConnectionState.NOT_CONFIGURED -> DesktopStrings.wizardHubLinkNotSetup()
                },
                body = when (bridgeState) {
                    BridgeConnectionState.PAIRED ->
                        DesktopStrings.wizardHubBodyPaired()
                    BridgeConnectionState.NOT_CONFIGURED ->
                        DesktopStrings.wizardHubBodyNotSetup()
                    else ->
                        DesktopStrings.wizardHubBodyElse()
                },
            )
            DesktopCard {
                Button(
                    onClick = { step = CompanionSetupStep.InstallWatch },
                    modifier = Modifier.fillMaxWidth(),
                    colors = desktopPrimaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text(DesktopStrings.wizardStepInstallWatch()) }
                OutlinedButton(
                    onClick = { step = CompanionSetupStep.InstallPhone },
                    modifier = Modifier.fillMaxWidth(),
                    colors = desktopSecondaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text(DesktopStrings.wizardStepInstallPhone()) }
                OutlinedButton(
                    onClick = { step = CompanionSetupStep.LinkPhone },
                    modifier = Modifier.fillMaxWidth(),
                    colors = desktopSecondaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text(DesktopStrings.wizardStepLinkPhone()) }
                Button(
                    onClick = onFinished,
                    modifier = Modifier.fillMaxWidth(),
                    colors = desktopPrimaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text(DesktopStrings.actionDone()) }
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
        Text(DesktopStrings.wizardLinkTitle(), style = MaterialTheme.typography.headlineLarge)
        Text(
            DesktopStrings.wizardLinkBody(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DesktopInfoPanel(
            title = DesktopStrings.wizardLinkNeedPhoneTitle(),
            body = DesktopStrings.wizardLinkNeedPhoneBody(),
        )
        DesktopCard {
            if (qrActive && qrBitmap != null) {
                Image(
                    bitmap = qrBitmap!!,
                    contentDescription = DesktopStrings.wizardLinkQrCd(),
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(DesktopDimens.radiusMedium)),
                )
                Text(DesktopStrings.wizardLinkWaiting(), fontWeight = FontWeight.Medium)
            } else {
                DesktopErrorPanel(
                    title = DesktopStrings.wizardLinkQrErrorTitle(),
                    body = DesktopStrings.wizardLinkQrErrorBody(),
                    primaryLabel = DesktopStrings.actionTryAgain(),
                    onPrimary = controller::showPairQr,
                    secondaryLabel = DesktopStrings.actionBack(),
                    onSecondary = onSkip,
                )
            }
            pairMessage?.let {
                Text(
                    DesktopMessageResolver.text(it).orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            bridgeMsg?.let {
                Text(
                    DesktopMessageResolver.text(it).orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DesktopDimens.rowGap)) {
                OutlinedButton(
                    onClick = {
                        controller.cancelPairQr()
                        controller.showPairQr()
                    },
                    colors = desktopSecondaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text(DesktopStrings.actionNewQr()) }
                if (bridgeState == BridgeConnectionState.PAIRED) {
                    Button(
                        onClick = onContinue,
                        colors = desktopPrimaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) { Text(DesktopStrings.actionDoneLinking()) }
                }
                TextButton(onClick = onSkip) { Text(DesktopStrings.actionBack()) }
            }
        }
    }
}
