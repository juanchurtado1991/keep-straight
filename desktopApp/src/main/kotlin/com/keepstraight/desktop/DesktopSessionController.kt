package com.keepstraight.desktop

import com.keepstraight.desktop.alert.DesktopAlerter
import com.keepstraight.desktop.alert.NativeDesktopNotifier
import com.keepstraight.desktop.bridge.JvmDesktopBridgeClient
import com.keepstraight.desktop.presentation.bridge.BridgeStore
import com.keepstraight.desktop.presentation.camera.CameraStore
import com.keepstraight.desktop.presentation.session.SessionStore
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.presentation.DesktopStatusAction
import com.keepstraight.shared.presentation.DesktopStatusPresentation
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.prefs.Preferences

class DesktopSessionController(
    private val prefs: Preferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val session = com.keepstraight.shared.domain.DesktopPostureSession()
    private val alerter = DesktopAlerter(
        soundEnabled = { _desktopSoundEnabled.value },
        notificationEnabled = { _desktopNotificationEnabled.value },
    )
    private val bridge = JvmDesktopBridgeClient(prefs)
    private val cameraStore = CameraStore(prefs, session, scope)
    private val sessionStore: SessionStore
    private val bridgeStore = BridgeStore(
        scope = scope,
        prefs = prefs,
        bridge = bridge,
        session = session,
        onBridgeCleared = { sessionStore.applyLocalSettings() },
    )

    init {
        sessionStore = SessionStore(
            prefs = prefs,
            scope = scope,
            session = session,
            alerter = alerter,
            bridgeStore = bridgeStore,
            cameraStore = cameraStore,
        )
        cameraStore.initIfConsentGranted()
        bridgeStore.startSyncIfConfigured()
    }

    val uiState get() = sessionStore.uiState
    val devices get() = cameraStore.devices
    val selectedDeviceId get() = cameraStore.selectedDeviceId

    val bridgeState get() = bridgeStore.bridgeState
    val bridgeHost get() = bridgeStore.bridgeHost
    val pairQrBitmap get() = bridgeStore.pairQrBitmap
    val qrPairingActive get() = bridgeStore.qrPairingActive
    val pairMessage get() = bridgeStore.pairMessage
    val bridgeActionMessage get() = bridgeStore.bridgeActionMessage
    val bridgeActionBusy get() = bridgeStore.bridgeActionBusy

    val showPreview: StateFlow<Boolean> = cameraStore.showPreview
    val calibrationUiActive: StateFlow<Boolean> = cameraStore.calibrationUiActive
    val previewBitmap: StateFlow<ImageBitmap?> = cameraStore.previewBitmap
    val lowPower: StateFlow<Boolean> = cameraStore.lowPower

    private val _openAtLogin = kotlinx.coroutines.flow.MutableStateFlow(LoginItemManager.isEnabled())
    val openAtLogin: StateFlow<Boolean> = _openAtLogin

    val openAtLoginAvailable: Boolean = LoginItemManager.isAvailable()

    private val _openAtLoginMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(
        if (LoginItemManager.isAvailable()) {
            null
        } else {
            "Couldn't find a launcher for Open at login on this machine."
        },
    )
    val openAtLoginMessage: StateFlow<String?> = _openAtLoginMessage

    private val _desktopSoundEnabled = kotlinx.coroutines.flow.MutableStateFlow(
        prefs.getBoolean("desktop_sound_enabled", false),
    )
    val desktopSoundEnabled: StateFlow<Boolean> = _desktopSoundEnabled

    private val _desktopNotificationEnabled = kotlinx.coroutines.flow.MutableStateFlow(
        prefs.getBoolean("desktop_notification_enabled", true),
    )
    val desktopNotificationEnabled: StateFlow<Boolean> = _desktopNotificationEnabled

    private val _startHiddenInTray = kotlinx.coroutines.flow.MutableStateFlow(
        prefs.getBoolean("start_hidden_in_tray", false),
    )
    val startHiddenInTray: StateFlow<Boolean> = _startHiddenInTray

    val statusPresentation: StateFlow<DesktopStatusPresentation> get() = sessionStore.statusPresentation

    fun onCameraConsentGranted() = cameraStore.onCameraConsentGranted()

    fun handleStatusAction(action: DesktopStatusAction) = sessionStore.handleStatusAction(action)

    fun setDesktopSoundEnabled(value: Boolean) {
        prefs.putBoolean("desktop_sound_enabled", value)
        _desktopSoundEnabled.value = value
    }

    fun setDesktopNotificationEnabled(value: Boolean) {
        prefs.putBoolean("desktop_notification_enabled", value)
        _desktopNotificationEnabled.value = value
    }

    fun sendTestDesktopNotification() {
        scope.launch(Dispatchers.IO) {
            val result = NativeDesktopNotifier.notify(
                "KeepStraight",
                "Test notification — if you see this, alerts will work in the tray.",
            )
            bridgeStore.setBridgeActionMessage(
                when {
                    result.shown && !result.limited -> "Test notification sent."
                    result.shown -> "Test notification: ${result.detail ?: "limited"}"
                    else -> "Test notification failed: ${result.detail ?: "unknown error"}"
                },
            )
        }
    }

    fun noteHiddenToTray() {
        if (prefs.getBoolean("tray_hint_shown", false)) return
        prefs.putBoolean("tray_hint_shown", true)
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val where = when {
            os.contains("mac") -> "the menu bar"
            os.contains("win") -> "the notification area"
            else -> "the system tray"
        }
        scope.launch {
            withContext(Dispatchers.IO) {
                NativeDesktopNotifier.notify(
                    "KeepStraight is still running",
                    "Click the KeepStraight icon in $where to bring the window back.",
                )
            }
        }
    }

    fun setStartHiddenInTray(value: Boolean) {
        prefs.putBoolean("start_hidden_in_tray", value)
        _startHiddenInTray.value = value
    }

    fun reconnectBridge() = bridgeStore.reconnectBridge()

    fun clearBridgeActionMessage() = bridgeStore.clearBridgeActionMessage()

    fun enterCalibrationUi() = sessionStore.enterCalibrationUi()

    fun exitCalibrationUi() = sessionStore.exitCalibrationUi()

    fun setShowPreview(value: Boolean) = cameraStore.setShowPreview(value)

    fun setLowPower(value: Boolean) = cameraStore.setLowPower(value)

    fun setOpenAtLogin(value: Boolean) {
        val result = LoginItemManager.setEnabled(value)
        _openAtLogin.value = result.enabled
        _openAtLoginMessage.value = result.message
    }

    fun setSensitivity(level: SensitivityLevel) = sessionStore.setSensitivity(level)

    fun setSlumpDurationMs(ms: Long) = sessionStore.setSlumpDurationMs(ms)

    fun setRepeatAlertMs(ms: Long) = sessionStore.setRepeatAlertMs(ms)

    fun selectCamera(deviceId: String) = cameraStore.selectCamera(deviceId)

    fun refreshCameras() = cameraStore.refreshCameras()

    fun retryCamera() = cameraStore.retryCamera()

    fun startSession() = sessionStore.startSession()

    fun stopSession() = sessionStore.stopSession()

    fun beginErectCalibration() = sessionStore.beginErectCalibration()

    fun beginSlumpCalibration() = sessionStore.beginSlumpCalibration()

    fun cancelCalibration() = sessionStore.cancelCalibration()

    fun pairPhone(host: String, code: String, onResult: (String) -> Unit) =
        bridgeStore.pairPhone(host, code, onResult)

    fun showPairQr() = bridgeStore.showPairQr()

    fun cancelPairQr() = bridgeStore.cancelPairQr()

    fun clearBridge() = bridgeStore.clearBridge()

    fun shutdown() {
        bridgeStore.shutdown()
        sessionStore.stopSession()
        cameraStore.shutdown()
        sessionStore.shutdown()
        bridge.close()
        scope.cancel()
    }
}
