package com.keepstraight.desktop.ui.wizard

import com.keepstraight.desktop.presentation.DesktopMessageKey
import com.keepstraight.desktop.presentation.UserMessage
import com.keepstraight.desktop.ui.i18n.DesktopMessageResolver
import com.keepstraight.desktop.ui.i18n.DesktopStrings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keepstraight.desktop.adb.AdbInstaller
import com.keepstraight.desktop.adb.AdbResult
import com.keepstraight.desktop.adb.WirelessAdbQr
import com.keepstraight.desktop.adb.WirelessPairOffer
import com.keepstraight.desktop.adb.pairConnectViaQr
import com.keepstraight.desktop.ui.DesktopCard
import com.keepstraight.desktop.ui.DesktopDimens
import com.keepstraight.desktop.ui.DesktopErrorPanel
import com.keepstraight.desktop.ui.DesktopInfoPanel
import com.keepstraight.desktop.ui.DesktopPage
import com.keepstraight.desktop.ui.desktopPrimaryButtonColors
import com.keepstraight.desktop.ui.desktopSecondaryButtonColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Wireless phone install: show QR → phone scans → install APK.
 * Copy stays short and non-technical. Watches can't scan, so they use
 * [WirelessWatchInstallStep] instead.
 */
@Composable
fun WirelessPhoneInstallStep(
    installer: AdbInstaller = remember { AdbInstaller() },
    onInstalled: () -> Unit,
    onSkip: () -> Unit,
    title: String? = null,
) {
    val screenTitle = title ?: DesktopStrings.wizardInstallPhoneTitle()
    val scope = rememberCoroutineScope()
    var offer by remember { mutableStateOf<WirelessPairOffer?>(null) }
    var statusKey by remember { mutableStateOf(DesktopMessageKey.WIZARD_PREPARING) }
    var busy by remember { mutableStateOf(false) }
    var adbError by remember { mutableStateOf<AdbResult.Err?>(null) }
    var unexpectedError by remember { mutableStateOf<UserMessage?>(null) }
    var pairJob by remember { mutableStateOf<Job?>(null) }

    fun clearError() {
        adbError = null
        unexpectedError = null
    }

    fun startPairing() {
        pairJob?.cancel()
        clearError()
        val next = WirelessAdbQr.createOffer()
        offer = next
        busy = true
        statusKey = DesktopMessageKey.WIZARD_SCAN_QR_PHONE
        pairJob = scope.launch {
            try {
                when (val connected = installer.pairConnectViaQr(next) { statusKey = it }) {
                    is AdbResult.Ok -> {
                        statusKey = DesktopMessageKey.WIZARD_INSTALLING_PHONE
                        when (val installed = installer.installPhoneApk(connected.value)) {
                            is AdbResult.Ok -> {
                                busy = false
                                statusKey = DesktopMessageKey.WIZARD_INSTALLED_PHONE
                                onInstalled()
                            }
                            is AdbResult.Err -> {
                                busy = false
                                adbError = installed
                            }
                        }
                    }
                    is AdbResult.Err -> {
                        busy = false
                        adbError = connected
                    }
                }
            } catch (exception: Exception) {
                busy = false
                unexpectedError = UserMessage(DesktopMessageKey.WIZARD_UNEXPECTED_ERROR)
                exception.printStackTrace()
            }
        }
    }

    DisposableEffect(Unit) {
        startPairing()
        onDispose { pairJob?.cancel() }
    }

    DesktopPage {
        Text(screenTitle, style = MaterialTheme.typography.headlineLarge)
        Text(
            DesktopStrings.wizardInstallPhoneBody(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        DesktopInfoPanel(
            title = DesktopStrings.wizardInstallPhoneOnPhoneTitle(),
            body = DesktopStrings.wizardInstallPhoneOnPhoneBody(),
        )

        DesktopCard {
            offer?.let { o ->
                Image(
                    bitmap = o.qrBitmap,
                    contentDescription = DesktopStrings.wizardInstallPhoneQrCd(),
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(DesktopDimens.radiusMedium))
                        .align(Alignment.CenterHorizontally),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesktopDimens.rowGap),
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
                Text(
                    DesktopMessageResolver.text(statusKey),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }

            adbError?.let { err ->
                DesktopErrorPanel(
                    title = DesktopMessageResolver.text(err.titleKey),
                    body = DesktopMessageResolver.text(err.bodyKey),
                    primaryLabel = DesktopStrings.actionTryAgain(),
                    onPrimary = { startPairing() },
                    secondaryLabel = DesktopStrings.actionSkipForNow(),
                    onSecondary = onSkip,
                )
            }

            unexpectedError?.let {
                DesktopErrorPanel(
                    title = DesktopMessageResolver.text(it.key),
                    body = DesktopMessageResolver.text(DesktopMessageKey.WIZARD_UNEXPECTED_ERROR_BODY_PHONE),
                    primaryLabel = DesktopStrings.actionTryAgain(),
                    onPrimary = { startPairing() },
                    secondaryLabel = DesktopStrings.actionSkipForNow(),
                    onSecondary = onSkip,
                )
            }

            Spacer(modifier = Modifier.height(DesktopDimens.itemGap))
            Row(horizontalArrangement = Arrangement.spacedBy(DesktopDimens.rowGap)) {
                if (busy) {
                    OutlinedButton(
                        onClick = {
                            pairJob?.cancel()
                            busy = false
                            statusKey = DesktopMessageKey.WIZARD_STOPPED_NEW_CODE
                        },
                        colors = desktopSecondaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) { Text(DesktopStrings.actionStopWaiting()) }
                } else {
                    Button(
                        onClick = { startPairing() },
                        colors = desktopPrimaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) { Text(DesktopStrings.actionShowNewCode()) }
                }
                TextButton(onClick = onSkip) { Text(DesktopStrings.wizardInstallPhoneSkip()) }
            }
            Text(
                DesktopStrings.wizardInstallPhoneSettingsHint(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
