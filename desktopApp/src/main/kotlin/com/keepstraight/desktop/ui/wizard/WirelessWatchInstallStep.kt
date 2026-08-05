package com.keepstraight.desktop.ui.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keepstraight.desktop.adb.AdbDevice
import com.keepstraight.desktop.adb.AdbInstaller
import com.keepstraight.desktop.adb.AdbResult
import com.keepstraight.desktop.adb.pairOrConnect
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
 * Watch install only. Independent from phone install and from KeepStraight phone↔desktop linking.
 * Prefers an already-connected watch (no typing); otherwise the user enters what the watch shows.
 */
@Composable
fun WirelessWatchInstallStep(
    installer: AdbInstaller = remember { AdbInstaller() },
    onInstalled: () -> Unit,
    onSkip: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var readyWatches by remember { mutableStateOf<List<AdbDevice>>(emptyList()) }
    var address by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Looking for a watch…") }
    var busy by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(true) }
    var showManual by remember { mutableStateOf(false) }
    var errorTitle by remember { mutableStateOf<String?>(null) }
    var errorBody by remember { mutableStateOf<String?>(null) }
    var errorDetail by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }

    fun clearError() {
        errorTitle = null
        errorBody = null
        errorDetail = null
    }

    fun showError(err: AdbResult.Err) {
        errorTitle = err.title
        errorBody = err.body
        errorDetail = err.detail
    }

    fun showUnexpected(e: Throwable) {
        errorTitle = "Something went wrong"
        errorBody = "Close Android Studio or other wireless-debug tools, then try Connect & install again."
        errorDetail = e.message?.takeIf { it.isNotBlank() }
    }

    fun refreshWatches() {
        job?.cancel()
        clearError()
        scanning = true
        status = "Looking for a watch already on Wi‑Fi…"
        job = scope.launch {
            try {
                when (val r = installer.listReadyWatches()) {
                    is AdbResult.Ok -> {
                        readyWatches = r.value
                        scanning = false
                        if (r.value.isNotEmpty()) {
                            showManual = false
                            status = "Watch ready — tap Install."
                        } else {
                            showManual = true
                            status = "No watch connected yet. Turn on Wireless debugging on the watch, then Search again — or type the address below."
                        }
                    }
                    is AdbResult.Err -> {
                        readyWatches = emptyList()
                        scanning = false
                        showManual = true
                        status = "Couldn’t scan devices — type what the watch shows."
                        showError(r)
                    }
                }
            } catch (e: Exception) {
                readyWatches = emptyList()
                scanning = false
                showManual = true
                status = "Couldn’t scan devices — type what the watch shows."
                showUnexpected(e)
            }
        }
    }

    fun installOnSerial(serial: String?) {
        job?.cancel()
        clearError()
        busy = true
        status = "Installing KeepStraight on the watch…"
        job = scope.launch {
            try {
                when (val installed = installer.installWearApk(serial)) {
                    is AdbResult.Ok -> {
                        busy = false
                        status = "Installed — opening KeepStraight on the watch"
                        onInstalled()
                    }
                    is AdbResult.Err -> {
                        busy = false
                        status = "Stopped."
                        showError(installed)
                    }
                }
            } catch (e: Exception) {
                busy = false
                status = "Stopped."
                showUnexpected(e)
            }
        }
    }

    fun installManual() {
        val raw = address.trim()
        val host = raw.substringBeforeLast(':', "").ifEmpty { raw }
        val port = raw.substringAfterLast(':', "").toIntOrNull()
        if (host.isEmpty() || port == null) {
            errorTitle = "That address doesn’t look right"
            errorBody = "Copy it exactly as the watch shows it, including the port — for example 192.168.1.42:37031."
            errorDetail = null
            return
        }
        job?.cancel()
        clearError()
        busy = true
        job = scope.launch {
            try {
                val connected = installer.pairOrConnect(
                    host = host,
                    port = port,
                    pairingCode = code.trim().takeIf { it.isNotEmpty() },
                ) { status = it }
                when (connected) {
                    is AdbResult.Err -> {
                        busy = false
                        status = "Stopped."
                        showError(connected)
                    }
                    is AdbResult.Ok -> {
                        status = "Installing KeepStraight on the watch…"
                        when (val installed = installer.installWearApk(connected.value)) {
                            is AdbResult.Ok -> {
                                busy = false
                                status = "Installed — opening KeepStraight on the watch"
                                onInstalled()
                            }
                            is AdbResult.Err -> {
                                busy = false
                                status = "Stopped."
                                showError(installed)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                busy = false
                status = "Stopped."
                showUnexpected(e)
            }
        }
    }

    DisposableEffect(Unit) {
        refreshWatches()
        onDispose { job?.cancel() }
    }

    DesktopPage {
        Text("Install on your watch", style = MaterialTheme.typography.headlineLarge)
        Text(
            "This only installs the watch app. It does not link your phone, and your phone doesn’t need to be involved.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        DesktopInfoPanel(
            title = "On the watch",
            body = "Settings → Developer options → Wireless debugging on, same Wi‑Fi as this computer. " +
                "If the watch is already connected here, just tap Install. Otherwise open Pair new device and enter the address/code below.",
        )

        DesktopCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesktopDimens.rowGap),
            ) {
                if (busy || scanning) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
                Text(status, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }

            if (readyWatches.isNotEmpty() && !busy) {
                readyWatches.forEach { watch ->
                    Button(
                        onClick = { installOnSerial(watch.serial) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = desktopPrimaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) {
                        Text("Install on ${watch.serial.substringBefore('.').take(24)}")
                    }
                }
            }

            if (showManual) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Watch address (IP:port)") },
                    placeholder = { Text("192.168.1.42:37031") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter { ch -> ch.isDigit() }.take(6) },
                    label = { Text("Pairing code (leave empty if none)") },
                    placeholder = { Text("123456") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                )
                Text(
                    "This is Wireless debugging pairing for the watch — not the KeepStraight phone QR.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            errorTitle?.let { titleText ->
                DesktopErrorPanel(
                    title = titleText,
                    body = errorBody.orEmpty(),
                    detail = errorDetail,
                    primaryLabel = "Try again",
                    onPrimary = {
                        if (readyWatches.isNotEmpty()) {
                            installOnSerial(readyWatches.first().serial)
                        } else if (address.isNotBlank()) {
                            installManual()
                        } else {
                            refreshWatches()
                        }
                    },
                    secondaryLabel = "Back",
                    onSecondary = onSkip,
                )
            }

            Spacer(modifier = Modifier.height(DesktopDimens.itemGap))
            Row(horizontalArrangement = Arrangement.spacedBy(DesktopDimens.rowGap)) {
                if (busy) {
                    OutlinedButton(
                        onClick = {
                            job?.cancel()
                            busy = false
                            status = "Stopped."
                        },
                        colors = desktopSecondaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) { Text("Stop") }
                } else {
                    if (showManual) {
                        Button(
                            onClick = { installManual() },
                            enabled = address.isNotBlank(),
                            colors = desktopPrimaryButtonColors(),
                            shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                        ) { Text("Connect & install") }
                    }
                    OutlinedButton(
                        onClick = { refreshWatches() },
                        enabled = !scanning,
                        colors = desktopSecondaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) { Text("Search again") }
                    if (!showManual) {
                        TextButton(onClick = { showManual = true }) { Text("Enter address manually") }
                    }
                }
                TextButton(onClick = onSkip) { Text("Back") }
            }
        }
    }
}
