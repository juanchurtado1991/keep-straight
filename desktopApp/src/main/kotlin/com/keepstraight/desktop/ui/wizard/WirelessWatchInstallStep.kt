package com.keepstraight.desktop.ui.wizard

import com.keepstraight.desktop.presentation.DesktopMessageKey
import com.keepstraight.desktop.presentation.UserMessage
import com.keepstraight.desktop.ui.i18n.DesktopMessageResolver
import com.keepstraight.desktop.ui.i18n.DesktopStrings
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
    var statusKey by remember { mutableStateOf(DesktopMessageKey.WIZARD_WATCH_LOOKUP) }
    var busy by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(true) }
    var showManual by remember { mutableStateOf(false) }
    var adbError by remember { mutableStateOf<AdbResult.Err?>(null) }
    var userError by remember { mutableStateOf<UserMessage?>(null) }
    var errorDetail by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }

    fun clearError() {
        adbError = null
        userError = null
        errorDetail = null
    }

    fun showError(err: AdbResult.Err) {
        adbError = err
        errorDetail = err.detail
    }

    fun showUnexpected(e: Throwable) {
        userError = UserMessage(DesktopMessageKey.WIZARD_UNEXPECTED_ERROR)
        errorDetail = e.message?.takeIf { it.isNotBlank() }
    }

    fun refreshWatches() {
        job?.cancel()
        clearError()
        scanning = true
        statusKey = DesktopMessageKey.WIZARD_WATCH_SCANNING
        job = scope.launch {
            try {
                when (val r = installer.listReadyWatches()) {
                    is AdbResult.Ok -> {
                        readyWatches = r.value
                        scanning = false
                        if (r.value.isNotEmpty()) {
                            showManual = false
                            statusKey = DesktopMessageKey.WIZARD_WATCH_READY
                        } else {
                            showManual = true
                            statusKey = DesktopMessageKey.WIZARD_WATCH_NOT_CONNECTED
                        }
                    }
                    is AdbResult.Err -> {
                        readyWatches = emptyList()
                        scanning = false
                        showManual = true
                        statusKey = DesktopMessageKey.WIZARD_WATCH_SCAN_FAILED
                        showError(r)
                    }
                }
            } catch (e: Exception) {
                readyWatches = emptyList()
                scanning = false
                showManual = true
                statusKey = DesktopMessageKey.WIZARD_WATCH_SCAN_FAILED
                showUnexpected(e)
            }
        }
    }

    fun installOnSerial(serial: String?) {
        job?.cancel()
        clearError()
        busy = true
        statusKey = DesktopMessageKey.WIZARD_INSTALLING_WATCH
        job = scope.launch {
            try {
                when (val installed = installer.installWearApk(serial)) {
                    is AdbResult.Ok -> {
                        busy = false
                        statusKey = DesktopMessageKey.WIZARD_INSTALLED_WATCH
                        onInstalled()
                    }
                    is AdbResult.Err -> {
                        busy = false
                        statusKey = DesktopMessageKey.WIZARD_STOPPED
                        showError(installed)
                    }
                }
            } catch (e: Exception) {
                busy = false
                statusKey = DesktopMessageKey.WIZARD_STOPPED
                showUnexpected(e)
            }
        }
    }

    fun installManual() {
        val raw = address.trim()
        val host = raw.substringBeforeLast(':', "").ifEmpty { raw }
        val port = raw.substringAfterLast(':', "").toIntOrNull()
        if (host.isEmpty() || port == null) {
            userError = UserMessage(DesktopMessageKey.WIZARD_ADDRESS_INVALID)
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
                ) { statusKey = it }
                when (connected) {
                    is AdbResult.Err -> {
                        busy = false
                        statusKey = DesktopMessageKey.WIZARD_STOPPED
                        showError(connected)
                    }
                    is AdbResult.Ok -> {
                        statusKey = DesktopMessageKey.WIZARD_INSTALLING_WATCH
                        when (val installed = installer.installWearApk(connected.value)) {
                            is AdbResult.Ok -> {
                                busy = false
                                statusKey = DesktopMessageKey.WIZARD_INSTALLED_WATCH
                                onInstalled()
                            }
                            is AdbResult.Err -> {
                                busy = false
                                statusKey = DesktopMessageKey.WIZARD_STOPPED
                                showError(installed)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                busy = false
                statusKey = DesktopMessageKey.WIZARD_STOPPED
                showUnexpected(e)
            }
        }
    }

    DisposableEffect(Unit) {
        refreshWatches()
        onDispose { job?.cancel() }
    }

    DesktopPage {
        Text(DesktopStrings.wizardInstallWatchTitle(), style = MaterialTheme.typography.headlineLarge)
        Text(
            DesktopStrings.wizardInstallWatchBody(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        DesktopInfoPanel(
            title = DesktopStrings.wizardInstallWatchOnWatchTitle(),
            body = DesktopStrings.wizardInstallWatchOnWatchBody(),
        )

        DesktopCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesktopDimens.rowGap),
            ) {
                if (busy || scanning) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
                Text(
                    DesktopMessageResolver.text(statusKey),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (readyWatches.isNotEmpty() && !busy) {
                readyWatches.forEach { watch ->
                    Button(
                        onClick = { installOnSerial(watch.serial) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = desktopPrimaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) {
                        Text(
                            DesktopStrings.wizardInstallOnDevice(
                                watch.serial.substringBefore('.').take(24),
                            ),
                        )
                    }
                }
            }

            if (showManual) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(DesktopStrings.wizardInstallWatchAddressLabel()) },
                    placeholder = { Text(DesktopStrings.wizardInstallWatchAddressPlaceholder()) },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter { ch -> ch.isDigit() }.take(6) },
                    label = { Text(DesktopStrings.wizardInstallWatchCodeLabel()) },
                    placeholder = { Text(DesktopStrings.wizardInstallWatchCodePlaceholder()) },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                )
                Text(
                    DesktopStrings.wizardInstallWatchPairingNote(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            adbError?.let { err ->
                DesktopErrorPanel(
                    title = DesktopMessageResolver.text(err.titleKey),
                    body = DesktopMessageResolver.text(err.bodyKey),
                    detail = errorDetail,
                    primaryLabel = DesktopStrings.actionTryAgain(),
                    onPrimary = {
                        if (readyWatches.isNotEmpty()) {
                            installOnSerial(readyWatches.first().serial)
                        } else if (address.isNotBlank()) {
                            installManual()
                        } else {
                            refreshWatches()
                        }
                    },
                    secondaryLabel = DesktopStrings.actionBack(),
                    onSecondary = onSkip,
                )
            }

            userError?.let { err ->
                val bodyKey = when (err.key) {
                    DesktopMessageKey.WIZARD_ADDRESS_INVALID -> DesktopMessageKey.WIZARD_ADDRESS_INVALID_BODY
                    else -> DesktopMessageKey.WIZARD_UNEXPECTED_ERROR_BODY_WATCH
                }
                DesktopErrorPanel(
                    title = DesktopMessageResolver.text(err.key),
                    body = DesktopMessageResolver.text(bodyKey),
                    detail = errorDetail,
                    primaryLabel = DesktopStrings.actionTryAgain(),
                    onPrimary = {
                        if (err.key == DesktopMessageKey.WIZARD_ADDRESS_INVALID) {
                            clearError()
                        } else if (readyWatches.isNotEmpty()) {
                            installOnSerial(readyWatches.first().serial)
                        } else if (address.isNotBlank()) {
                            installManual()
                        } else {
                            refreshWatches()
                        }
                    },
                    secondaryLabel = DesktopStrings.actionBack(),
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
                            statusKey = DesktopMessageKey.WIZARD_STOPPED
                        },
                        colors = desktopSecondaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) { Text(DesktopStrings.actionStop()) }
                } else {
                    if (showManual) {
                        Button(
                            onClick = { installManual() },
                            enabled = address.isNotBlank(),
                            colors = desktopPrimaryButtonColors(),
                            shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                        ) { Text(DesktopStrings.actionConnectInstall()) }
                    }
                    OutlinedButton(
                        onClick = { refreshWatches() },
                        enabled = !scanning,
                        colors = desktopSecondaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) { Text(DesktopStrings.actionSearchAgain()) }
                    if (!showManual) {
                        TextButton(onClick = { showManual = true }) {
                            Text(DesktopStrings.actionEnterAddressManually())
                        }
                    }
                }
                TextButton(onClick = onSkip) { Text(DesktopStrings.actionBack()) }
            }
        }
    }
}
