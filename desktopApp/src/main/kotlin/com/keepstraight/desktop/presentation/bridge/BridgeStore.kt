package com.keepstraight.desktop.presentation.bridge

import androidx.compose.ui.graphics.ImageBitmap
import com.keepstraight.desktop.CalibrationStore
import com.keepstraight.desktop.bridge.DesktopPairAssistServer
import com.keepstraight.desktop.bridge.JvmDesktopBridgeClient
import com.keepstraight.desktop.ui.QrCodeBitmap
import com.keepstraight.desktop.bridge.BridgeClientException
import com.keepstraight.desktop.bridge.isUnauthorized
import com.keepstraight.desktop.presentation.BridgeErrorMapper
import com.keepstraight.desktop.presentation.DesktopMessageKey
import com.keepstraight.desktop.presentation.DesktopPrefsKeys
import com.keepstraight.desktop.presentation.UserMessage
import com.keepstraight.desktop.ui.i18n.DesktopMessageJvm
import com.keepstraight.shared.bridge.BridgeProtocolError
import com.keepstraight.shared.bridge.DesktopLanProtocol
import com.keepstraight.shared.bridge.DesktopPairingQr
import com.keepstraight.shared.bridge.DesktopSlumpEvent
import com.keepstraight.shared.bridge.DesktopSlumpEventType
import com.keepstraight.shared.bridge.PhoneHelloRequest
import com.keepstraight.shared.bridge.PhoneHelloResponse
import com.keepstraight.shared.domain.DesktopAlertEvent
import com.keepstraight.shared.domain.DesktopPostureSession
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.presentation.BridgeConnectionState
import com.keepstraight.shared.presentation.DesktopIssue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.prefs.Preferences

class BridgeStore(
    private val scope: CoroutineScope,
    private val prefs: Preferences,
    private val bridge: JvmDesktopBridgeClient,
    private val session: DesktopPostureSession,
    private val onBridgeCleared: () -> Unit,
) {
    private var settingsSyncJob: Job? = null
    private var workSampleSyncJob: Job? = null
    private var pairAssist: DesktopPairAssistServer? = null
    private var pairAssistStopJob: Job? = null
    private val pairingMutex = Mutex()
    private var phoneAlertsEnabled: Boolean = true

    private val _bridgeState = MutableStateFlow(
        if (bridge.isConfigured) BridgeConnectionState.PAIRED else BridgeConnectionState.NOT_CONFIGURED,
    )
    val bridgeState: StateFlow<BridgeConnectionState> = _bridgeState.asStateFlow()

    private val _bridgeHost = MutableStateFlow(
        if (bridge.isConfigured) prefs.get(DesktopPrefsKeys.BRIDGE_HOST, "") else "",
    )
    val bridgeHost: StateFlow<String> = _bridgeHost.asStateFlow()

    private val _pairQrBitmap = MutableStateFlow<ImageBitmap?>(null)
    val pairQrBitmap: StateFlow<ImageBitmap?> = _pairQrBitmap.asStateFlow()

    private val _qrPairingActive = MutableStateFlow(false)
    val qrPairingActive: StateFlow<Boolean> = _qrPairingActive.asStateFlow()

    private val _pairMessage = MutableStateFlow<UserMessage?>(null)
    val pairMessage: StateFlow<UserMessage?> = _pairMessage.asStateFlow()

    private val _bridgeActionMessage = MutableStateFlow<UserMessage?>(null)
    val bridgeActionMessage: StateFlow<UserMessage?> = _bridgeActionMessage.asStateFlow()

    private val _bridgeActionBusy = MutableStateFlow(false)
    val bridgeActionBusy: StateFlow<Boolean> = _bridgeActionBusy.asStateFlow()

    fun startSyncIfConfigured() {
        if (bridge.isConfigured) {
            startSettingsSync()
            startWorkSampleSync()
        }
    }

    /** Ping + settings fetch; surfaces a clear message on failure. */
    fun reconnectBridge() {
        if (_bridgeActionBusy.value) return
        if (!bridge.isConfigured) {
            _bridgeState.value = BridgeConnectionState.NOT_CONFIGURED
            _bridgeActionMessage.value = UserMessage(DesktopMessageKey.BRIDGE_NOT_LINKED_SETUP)
            return
        }
        _bridgeActionBusy.value = true
        _bridgeActionMessage.value = UserMessage(DesktopMessageKey.BRIDGE_CHECKING)
        scope.launch {
            val ping = bridge.ping()
            if (ping.isFailure) {
                _bridgeState.value = BridgeConnectionState.DEGRADED
                _bridgeActionMessage.value = UserMessage(DesktopMessageKey.BRIDGE_CANT_REACH)
                _bridgeActionBusy.value = false
                return@launch
            }
            syncSettingsFromPhone()
            if (_bridgeState.value == BridgeConnectionState.FAILED) {
                _bridgeActionMessage.value = UserMessage(DesktopMessageKey.BRIDGE_REJECTED)
            } else if (_bridgeState.value == BridgeConnectionState.DEGRADED) {
                _bridgeActionMessage.value = UserMessage(DesktopMessageKey.BRIDGE_SYNC_TROUBLE)
            } else {
                _bridgeState.value = BridgeConnectionState.PAIRED
                _bridgeActionMessage.value = UserMessage(DesktopMessageKey.BRIDGE_LOOKS_GOOD)
                startSettingsSync()
                startWorkSampleSync()
            }
            _bridgeActionBusy.value = false
        }
    }

    fun clearBridgeActionMessage() {
        _bridgeActionMessage.value = null
    }

    fun setBridgeActionMessage(message: UserMessage?) {
        _bridgeActionMessage.value = message
    }

    fun pairPhone(host: String, code: String, onResult: (UserMessage) -> Unit) {
        if (host.isBlank() || code.isBlank()) {
            session.setIssue(DesktopIssue.BridgePairFailed(""))
            onResult(UserMessage(DesktopMessageKey.BRIDGE_MISSING_HOST_CODE))
            return
        }
        scope.launch {
            completePairFromPhone(host, DesktopLanProtocol.DEFAULT_PORT, code, onResult)
        }
    }

    /** Binding/unbinding the assist server blocks, so it never runs on the UI thread. */
    fun showPairQr() {
        val previous = pairAssist
        pairAssist = null
        _pairQrBitmap.value = null
        _pairMessage.value = UserMessage(DesktopMessageKey.BRIDGE_GETTING_CODE)
        _qrPairingActive.value = true
        scope.launch {
            awaitPairAssistStopped(previous)
            val assist = DesktopPairAssistServer { hello -> handlePhoneHello(hello) }
            val offer = withContext(Dispatchers.IO) { assist.start() }
            pairAssist = assist
            _pairQrBitmap.value = QrCodeBitmap.encode(DesktopPairingQr.encode(offer))
            _pairMessage.value = UserMessage(DesktopMessageKey.BRIDGE_SCAN_QR)
        }
    }

    fun cancelPairQr() {
        val assist = pairAssist
        pairAssist = null
        _pairQrBitmap.value = null
        _qrPairingActive.value = false
        pairAssistStopJob = scope.launch {
            withContext(Dispatchers.IO) { assist?.stop() }
        }
    }

    fun clearBridge() {
        cancelPairQr()
        stopSettingsSync()
        stopWorkSampleSync()
        bridge.clear()
        _bridgeHost.value = ""
        _bridgeState.value = BridgeConnectionState.NOT_CONFIGURED
        session.clearPhoneSettingsFlag()
        session.clearIssue()
        // Desktop owns its settings again, and a future phone must not inherit the old
        // "alerts off" answer before its first settings sync.
        phoneAlertsEnabled = true
        onBridgeCleared()
        _pairMessage.value = UserMessage(DesktopMessageKey.BRIDGE_UNLINKED)
    }

    fun shutdown() {
        cancelPairQr()
        stopSettingsSync()
        stopWorkSampleSync()
        bridge.close()
    }

    suspend fun forwardAlert(event: DesktopAlertEvent) {
        if (!bridge.isConfigured) {
            System.err.println("KeepStraight: alert not forwarded — phone not linked")
            return
        }
        if (!phoneAlertsEnabled) {
            System.err.println("KeepStraight: alert not forwarded — phone Alerts toggle is off")
            return
        }
        val type = when (event) {
            DesktopAlertEvent.SLUMP_INITIAL -> DesktopSlumpEventType.SLUMP_INITIAL
            DesktopAlertEvent.SLUMP_REPEAT -> DesktopSlumpEventType.SLUMP_REPEAT
        }
        val ui = session.uiState.value
        val result = bridge.sendEvent(
            DesktopSlumpEvent(
                type = type,
                slumpScore = ui.slumpScore,
                presence = ui.presence.name,
                timestampMs = System.currentTimeMillis(),
            ),
        )
        result.onFailure { err ->
            val unauthorized = (err as? BridgeClientException)?.isUnauthorized() == true
            _bridgeState.value = if (unauthorized) {
                BridgeConnectionState.FAILED
            } else {
                BridgeConnectionState.DEGRADED
            }
            session.setIssue(
                if (unauthorized) DesktopIssue.BridgeUnauthorized else DesktopIssue.BridgeSendFailed,
            )
        }.onSuccess {
            if (_bridgeState.value != BridgeConnectionState.PAIRED) {
                _bridgeState.value = BridgeConnectionState.PAIRED
            }
            if (session.uiState.value.issue is DesktopIssue.BridgeSendFailed ||
                session.uiState.value.issue is DesktopIssue.BridgeUnauthorized
            ) {
                session.clearIssue()
            }
        }
    }

    fun emitSessionEvent(type: DesktopSlumpEventType) {
        if (!bridge.isConfigured) return
        scope.launch {
            bridge.sendEvent(
                DesktopSlumpEvent(
                    type = type,
                    presence = session.uiState.value.presence.name,
                    timestampMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    private suspend fun awaitPairAssistStopped(previous: DesktopPairAssistServer?) {
        pairAssistStopJob?.join()
        pairAssistStopJob = null
        previous?.let { withContext(Dispatchers.IO) { it.stop() } }
    }

    private suspend fun handlePhoneHello(hello: PhoneHelloRequest): PhoneHelloResponse = pairingMutex.withLock {
        var lastError: UserMessage = UserMessage(DesktopMessageKey.BRIDGE_PAIRING_FAILED)
        var lastProtocolError = BridgeProtocolError.PAIRING_FAILED
        for (host in hello.phoneHosts) {
            val result = bridge.pair(host, hello.phonePort, hello.code)
            if (result.isSuccess) {
                _bridgeHost.value = host
                _bridgeState.value = BridgeConnectionState.PAIRED
                session.clearIssue()
                startSettingsSync()
                startWorkSampleSync()
                _pairMessage.value = UserMessage(DesktopMessageKey.BRIDGE_PAIRED_WITH, listOf(host))
                scope.launch {
                    delay(150)
                    cancelPairQr()
                }
                return@withLock PhoneHelloResponse(ok = true)
            }
            lastError = bridgeFailureMessage(result.exceptionOrNull())
            lastProtocolError = (result.exceptionOrNull() as? BridgeClientException)?.protocolError
                ?: BridgeProtocolError.PAIRING_FAILED
        }
        session.setIssue(
            DesktopIssue.BridgePairFailed(userMessageText(lastError)),
        )
        _pairMessage.value = lastError
        return@withLock PhoneHelloResponse(
            ok = false,
            errorCode = lastProtocolError,
        )
    }

    private suspend fun completePairFromPhone(
        host: String,
        port: Int,
        code: String,
        onResult: (UserMessage) -> Unit,
    ) {
        val result = bridge.pair(host, port, code)
        withContext(Dispatchers.Main) {
            result.fold(
                onSuccess = {
                    _bridgeHost.value = host
                    _bridgeState.value = BridgeConnectionState.PAIRED
                    session.clearIssue()
                    startSettingsSync()
                    startWorkSampleSync()
                    onResult(UserMessage(DesktopMessageKey.BRIDGE_PAIRED_SYNCING))
                },
                onFailure = { err ->
                    _bridgeState.value = BridgeConnectionState.FAILED
                    val message = bridgeFailureMessage(err)
                    session.setIssue(DesktopIssue.BridgePairFailed(userMessageText(message)))
                    onResult(message)
                },
            )
        }
    }

    private fun bridgeFailureMessage(err: Throwable?): UserMessage = BridgeErrorMapper.userMessage(err)

    private fun userMessageText(message: UserMessage): String =
        message.override ?: DesktopMessageJvm.text(message.key, *message.args.toTypedArray())

    private fun startSettingsSync() {
        if (settingsSyncJob?.isActive == true) return
        settingsSyncJob = scope.launch {
            while (isActive && bridge.isConfigured) {
                syncSettingsFromPhone()
                delay(SETTINGS_SYNC_MS)
            }
        }
    }

    private fun stopSettingsSync() {
        settingsSyncJob?.cancel()
        settingsSyncJob = null
    }

    private fun startWorkSampleSync() {
        if (workSampleSyncJob?.isActive == true) return
        workSampleSyncJob = scope.launch {
            while (isActive && bridge.isConfigured) {
                flushWorkSample()
                delay(WORK_SAMPLE_SYNC_MS)
            }
        }
    }

    private fun stopWorkSampleSync() {
        workSampleSyncJob?.cancel()
        workSampleSyncJob = null
    }

    private suspend fun flushWorkSample() {
        val sample = session.drainWorkSample() ?: return
        val (seatedSec, goodSec) = sample
        val result = bridge.sendEvent(
            DesktopSlumpEvent(
                type = DesktopSlumpEventType.WORK_SAMPLE,
                presence = session.uiState.value.presence.name,
                timestampMs = System.currentTimeMillis(),
                seatedDeltaSec = seatedSec,
                goodPostureDeltaSec = goodSec,
            ),
        )
        result.onFailure {
            session.restoreWorkSample(seatedSec, goodSec)
        }
    }

    private suspend fun syncSettingsFromPhone() {
        val result = bridge.fetchSettings()
        result.onSuccess { settings ->
            val level = runCatching {
                SensitivityLevel.valueOf(settings.sensitivity)
            }.getOrDefault(SensitivityLevel.NORMAL)
            phoneAlertsEnabled = settings.alertsEnabled
            session.applyPhoneSettings(
                sensitivity = level,
                slumpDurationThresholdMs = settings.slumpDurationThresholdMs,
                repeatAlertIntervalMs = settings.repeatAlertIntervalMs,
            )
            prefs.put(DesktopPrefsKeys.SENSITIVITY, level.name)
            prefs.putLong(DesktopPrefsKeys.SLUMP_DURATION_MS, settings.slumpDurationThresholdMs)
            prefs.putLong(DesktopPrefsKeys.REPEAT_ALERT_MS, settings.repeatAlertIntervalMs)
            session.currentCalibration()?.let { CalibrationStore.save(prefs, it) }
            if (_bridgeState.value == BridgeConnectionState.DEGRADED) {
                _bridgeState.value = BridgeConnectionState.PAIRED
            }
        }.onFailure { err ->
            val unauthorized = (err as? BridgeClientException)?.isUnauthorized() == true
            if (unauthorized) {
                _bridgeState.value = BridgeConnectionState.FAILED
                session.setIssue(DesktopIssue.BridgeUnauthorized)
                stopSettingsSync()
                stopWorkSampleSync()
            } else if (_bridgeState.value == BridgeConnectionState.PAIRED) {
                _bridgeState.value = BridgeConnectionState.DEGRADED
            }
        }
    }

    private companion object {
        const val SETTINGS_SYNC_MS = 10_000L
        const val WORK_SAMPLE_SYNC_MS = 30_000L
    }
}
