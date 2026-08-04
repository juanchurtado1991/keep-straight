package com.keepstraight.desktop.ui.wizard

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
    title: String = "Install on your phone",
) {
    val scope = rememberCoroutineScope()
    var offer by remember { mutableStateOf<WirelessPairOffer?>(null) }
    var status by remember { mutableStateOf("Preparing…") }
    var busy by remember { mutableStateOf(false) }
    var errorTitle by remember { mutableStateOf<String?>(null) }
    var errorBody by remember { mutableStateOf<String?>(null) }
    var errorDetail by remember { mutableStateOf<String?>(null) }
    var pairJob by remember { mutableStateOf<Job?>(null) }

    fun clearError() {
        errorTitle = null
        errorBody = null
        errorDetail = null
    }

    fun startPairing() {
        pairJob?.cancel()
        clearError()
        val next = WirelessAdbQr.createOffer()
        offer = next
        busy = true
        status = "Scan this code with your phone"
        pairJob = scope.launch {
            when (val connected = installer.pairConnectViaQr(next) { status = it }) {
                is AdbResult.Ok -> {
                    status = "Installing KeepStraight…"
                    when (val installed = installer.installPhoneApk(connected.value)) {
                        is AdbResult.Ok -> {
                            busy = false
                            status = "Installed — opening KeepStraight on your phone"
                            onInstalled()
                        }
                        is AdbResult.Err -> {
                            busy = false
                            errorTitle = installed.title
                            errorBody = installed.body
                            errorDetail = installed.detail
                        }
                    }
                }
                is AdbResult.Err -> {
                    busy = false
                    errorTitle = connected.title
                    errorBody = connected.body
                    errorDetail = connected.detail
                }
            }
        }
    }

    DisposableEffect(Unit) {
        startPairing()
        onDispose { pairJob?.cancel() }
    }

    DesktopPage {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(
            "No cables. Your phone and this computer need the same Wi‑Fi.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        DesktopInfoPanel(
            title = "On your phone",
            body = "Settings → Developer options → Wireless debugging → Pair device with QR code. Then point the phone at the code below.",
        )

        DesktopCard {
            offer?.let { o ->
                Image(
                    bitmap = o.qrBitmap,
                    contentDescription = "QR code to install KeepStraight on your phone",
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
                Text(status, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }

            errorTitle?.let { titleText ->
                DesktopErrorPanel(
                    title = titleText,
                    body = errorBody.orEmpty(),
                    detail = errorDetail,
                    primaryLabel = "Try again",
                    onPrimary = { startPairing() },
                    secondaryLabel = "Skip for now",
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
                            status = "Stopped. Show a new code when you’re ready."
                        },
                        colors = desktopSecondaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) { Text("Stop waiting") }
                } else {
                    Button(
                        onClick = { startPairing() },
                        colors = desktopPrimaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) { Text("Show a new code") }
                }
                TextButton(onClick = onSkip) { Text("Skip — already installed, or later") }
            }
            Text(
                "You can install the phone app anytime from Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
