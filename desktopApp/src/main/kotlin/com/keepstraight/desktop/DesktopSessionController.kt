package com.keepstraight.desktop

import com.keepstraight.desktop.alert.DesktopAlerter
import com.keepstraight.desktop.alert.NativeDesktopNotifier
import com.keepstraight.desktop.bridge.JvmDesktopBridgeClient
import com.keepstraight.desktop.presentation.bridge.BridgeStore
import com.keepstraight.desktop.presentation.camera.CameraStore
import com.keepstraight.desktop.presentation.session.SessionStore
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.desktop.presentation.DesktopMessageKey
import com.keepstraight.desktop.presentation.DesktopPrefsKeys
import com.keepstraight.desktop.presentation.UserMessage
import com.keepstraight.desktop.ui.i18n.DesktopMessageJvm
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

    private val _openAtLoginMessage = kotlinx.coroutines.flow.MutableStateFlow<UserMessage?>(
        if (LoginItemManager.isAvailable()) {
            null
        } else {
            UserMessage(DesktopMessageKey.LOGIN_OPEN_AT_UNAVAILABLE)
        },
    )
    val openAtLoginMessage: StateFlow<UserMessage?> = _openAtLoginMessage

    private val _desktopSoundEnabled = kotlinx.coroutines.flow.MutableStateFlow(
        prefs.getBoolean(DesktopPrefsKeys.DESKTOP_SOUND_ENABLED, false),
    )
    val desktopSoundEnabled: StateFlow<Boolean> = _desktopSoundEnabled

    private val _desktopNotificationEnabled = kotlinx.coroutines.flow.MutableStateFlow(
        prefs.getBoolean(DesktopPrefsKeys.DESKTOP_NOTIFICATION_ENABLED, true),
    )
    val desktopNotificationEnabled: StateFlow<Boolean> = _desktopNotificationEnabled

    private val _startHiddenInTray = kotlinx.coroutines.flow.MutableStateFlow(
        prefs.getBoolean(DesktopPrefsKeys.START_HIDDEN_IN_TRAY, false),
    )
    val startHiddenInTray: StateFlow<Boolean> = _startHiddenInTray

    val statusPresentation: StateFlow<DesktopStatusPresentation> get() = sessionStore.statusPresentation

    fun onCameraConsentGranted() = cameraStore.onCameraConsentGranted()

    fun handleStatusAction(action: DesktopStatusAction) = sessionStore.handleStatusAction(action)

    fun setDesktopSoundEnabled(value: Boolean) {
        prefs.putBoolean(DesktopPrefsKeys.DESKTOP_SOUND_ENABLED, value)
        _desktopSoundEnabled.value = value
    }

    fun setDesktopNotificationEnabled(value: Boolean) {
        prefs.putBoolean(DesktopPrefsKeys.DESKTOP_NOTIFICATION_ENABLED, value)
        _desktopNotificationEnabled.value = value
    }

    fun sendTestDesktopNotification() {
        scope.launch(Dispatchers.IO) {
            val result = NativeDesktopNotifier.notify(
                DesktopMessageJvm.text(DesktopMessageKey.TEST_NOTIFICATION_TITLE),
                DesktopMessageJvm.text(DesktopMessageKey.TEST_NOTIFICATION_BODY),
            )
            bridgeStore.setBridgeActionMessage(
                when {
                    result.shown && !result.limited -> UserMessage(DesktopMessageKey.TEST_NOTIFICATION_SENT)
                    result.shown -> UserMessage(
                        result.detailKey ?: DesktopMessageKey.TEST_NOTIFICATION_LIMITED,
                    )
                    else -> UserMessage(
                        result.detailKey ?: DesktopMessageKey.TEST_NOTIFICATION_FAILED,
                    )
                },
            )
        }
    }

    fun noteHiddenToTray() {
        if (prefs.getBoolean(DesktopPrefsKeys.TRAY_HINT_SHOWN, false)) return
        prefs.putBoolean(DesktopPrefsKeys.TRAY_HINT_SHOWN, true)
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val whereKey = when {
            os.contains("mac") -> DesktopMessageKey.TRAY_WHERE_MENU_BAR
            os.contains("win") -> DesktopMessageKey.TRAY_WHERE_NOTIFICATION
            else -> DesktopMessageKey.TRAY_WHERE_SYSTEM_TRAY
        }
        scope.launch {
            withContext(Dispatchers.IO) {
                NativeDesktopNotifier.notify(
                    DesktopMessageJvm.text(DesktopMessageKey.TRAY_STILL_RUNNING),
                    DesktopMessageJvm.text(DesktopMessageKey.TRAY_CLICK_ICON, DesktopMessageJvm.text(whereKey)),
                )
            }
        }
    }

    fun setStartHiddenInTray(value: Boolean) {
        prefs.putBoolean(DesktopPrefsKeys.START_HIDDEN_IN_TRAY, value)
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
        _openAtLoginMessage.value = result.messageKey?.let {
            UserMessage(it, override = result.override)
        }
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

    fun pairPhone(host: String, code: String, onResult: (UserMessage) -> Unit) =
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
