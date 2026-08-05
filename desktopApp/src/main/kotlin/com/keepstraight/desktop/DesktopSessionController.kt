package com.keepstraight.desktop

import com.keepstraight.desktop.alert.DesktopAlerter
import com.keepstraight.desktop.alert.NativeDesktopNotifier
import com.keepstraight.desktop.bridge.JvmDesktopBridgeClient
import com.keepstraight.desktop.presentation.bridge.BridgeStore
import com.keepstraight.desktop.ui.FramePreview
import com.keepstraight.shared.bridge.DesktopSlumpEventType
import com.keepstraight.shared.domain.CalibrationPhase
import com.keepstraight.shared.domain.DesktopPostureSession
import com.keepstraight.shared.domain.DesktopSessionPhase
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.presentation.DesktopStatusAction
import com.keepstraight.shared.presentation.DesktopStatusMapper
import com.keepstraight.shared.presentation.DesktopStatusPresentation
import androidx.compose.ui.graphics.ImageBitmap
import com.keepstraight.shared.vision.CameraFrameSource
import com.keepstraight.shared.vision.JvmCameraFrameSource
import com.keepstraight.shared.vision.MissingModelPoseEstimator
import com.keepstraight.shared.vision.PoseEstimator
import com.keepstraight.shared.vision.VisionPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.prefs.Preferences

class DesktopSessionController(
    private val prefs: Preferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val session = DesktopPostureSession()
    private val alerter = DesktopAlerter(
        soundEnabled = { _desktopSoundEnabled.value },
        notificationEnabled = { _desktopNotificationEnabled.value },
    )
    private val bridge = JvmDesktopBridgeClient(prefs)
    private val bridgeStore = BridgeStore(
        scope = scope,
        prefs = prefs,
        bridge = bridge,
        session = session,
        onBridgeCleared = ::applyLocalSettings,
    )

    private var camera: CameraFrameSource? = null
    private var poseEstimator: PoseEstimator? = null
    private var pipelineJob: Job? = null
    private val visionLock = Any()
    private var lastTargetFps: Int = 5
    private var modelBytesLoaded: Boolean = false

    val uiState = session.uiState
    val devices get() = camera?.devices
    val selectedDeviceId get() = camera?.selectedDeviceId

    val bridgeState get() = bridgeStore.bridgeState
    val bridgeHost get() = bridgeStore.bridgeHost
    val pairQrBitmap get() = bridgeStore.pairQrBitmap
    val qrPairingActive get() = bridgeStore.qrPairingActive
    val pairMessage get() = bridgeStore.pairMessage
    val bridgeActionMessage get() = bridgeStore.bridgeActionMessage
    val bridgeActionBusy get() = bridgeStore.bridgeActionBusy

    private val _showPreview = MutableStateFlow(prefs.getBoolean("show_preview", false))
    val showPreview: StateFlow<Boolean> = _showPreview.asStateFlow()

    /** Full-screen calibration UI forces live preview regardless of the home toggle. */
    private val _calibrationUiActive = MutableStateFlow(false)
    val calibrationUiActive: StateFlow<Boolean> = _calibrationUiActive.asStateFlow()

    private val _previewBitmap = MutableStateFlow<ImageBitmap?>(null)
    val previewBitmap: StateFlow<ImageBitmap?> = _previewBitmap.asStateFlow()

    private val _lowPower = MutableStateFlow(prefs.getBoolean("low_power", false))
    val lowPower: StateFlow<Boolean> = _lowPower.asStateFlow()

    private val _openAtLogin = MutableStateFlow(LoginItemManager.isEnabled())
    val openAtLogin: StateFlow<Boolean> = _openAtLogin.asStateFlow()

    val openAtLoginAvailable: Boolean = LoginItemManager.isAvailable()

    private val _openAtLoginMessage = MutableStateFlow<String?>(
        if (LoginItemManager.isAvailable()) {
            null
        } else {
            "Couldn't find a launcher for Open at login on this machine."
        },
    )
    val openAtLoginMessage: StateFlow<String?> = _openAtLoginMessage.asStateFlow()

    private val _desktopSoundEnabled = MutableStateFlow(prefs.getBoolean("desktop_sound_enabled", false))
    val desktopSoundEnabled: StateFlow<Boolean> = _desktopSoundEnabled.asStateFlow()

    private val _desktopNotificationEnabled =
        MutableStateFlow(prefs.getBoolean("desktop_notification_enabled", true))
    val desktopNotificationEnabled: StateFlow<Boolean> = _desktopNotificationEnabled.asStateFlow()

    private val _startHiddenInTray = MutableStateFlow(prefs.getBoolean("start_hidden_in_tray", false))
    val startHiddenInTray: StateFlow<Boolean> = _startHiddenInTray.asStateFlow()

    val statusPresentation: StateFlow<DesktopStatusPresentation> = combine(
        session.uiState,
        bridgeStore.bridgeState,
    ) { ui, bridge ->
        DesktopStatusMapper.present(
            phase = ui.phase,
            presence = ui.presence,
            calibrationPhase = ui.calibrationPhase,
            hasCalibration = ui.hasCalibration,
            isSlumped = ui.isSlumped,
            slumpScore = ui.slumpScore,
            statusMessage = ui.statusMessage,
            issue = ui.issue,
            bridgeState = bridge,
            modelReady = ui.modelReady,
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = DesktopStatusMapper.present(
            phase = DesktopSessionPhase.IDLE,
            presence = com.keepstraight.shared.domain.PresenceState.AWAY,
            calibrationPhase = CalibrationPhase.NONE,
            hasCalibration = false,
            isSlumped = false,
            slumpScore = 0f,
            statusMessage = "Start a session to monitor posture.",
            issue = null,
            bridgeState = bridgeStore.bridgeState.value,
            modelReady = true,
        ),
    )

    init {
        session.onAlert = { event ->
            scope.launch(Dispatchers.IO) {
                alerter.alert(event)
                bridgeStore.forwardAlert(event)
            }
        }
        session.onCalibrationSaved = { cal ->
            CalibrationStore.save(prefs, cal)
        }

        applyLocalSettings()
        CalibrationStore.load(prefs)?.let { session.setCalibration(it) }

        if (prefs.getBoolean("camera_consent_accepted", false)) {
            warmUpVision()
        }
        bridgeStore.startSyncIfConfigured()
    }

    private fun applyLocalSettings() {
        val sensitivity = runCatching {
            SensitivityLevel.valueOf(prefs.get("sensitivity", SensitivityLevel.NORMAL.name))
        }.getOrDefault(SensitivityLevel.NORMAL)
        session.updateSensitivity(sensitivity)
        session.updateTimers(
            slumpDurationThresholdMs = prefs.getLong("slump_duration_ms", 30_000L),
            repeatAlertIntervalMs = prefs.getLong("repeat_alert_ms", 5_000L),
        )
    }

    fun onCameraConsentGranted() {
        prefs.putBoolean("camera_consent_accepted", true)
        warmUpVision()
    }

    private fun warmUpVision() {
        // Loading the pose model and enumerating webcams takes seconds — keep off the UI thread.
        scope.launch {
            ensurePose()
            ensureCamera()
            if (_showPreview.value) {
                startCameraPipeline()
            }
        }
    }

    fun handleStatusAction(action: DesktopStatusAction) {
        when (action) {
            DesktopStatusAction.RETRY_CAMERA -> retryCamera()
            DesktopStatusAction.REFRESH_CAMERAS -> refreshCameras()
            DesktopStatusAction.CALIBRATE,
            DesktopStatusAction.CALIBRATE_ERECT,
            DesktopStatusAction.CALIBRATE_SLUMP,
            -> {
                // Navigation to CalibrationScreen is handled by the UI layer.
            }
            DesktopStatusAction.STOP_SESSION -> stopSession()
            DesktopStatusAction.CLEAR_BRIDGE -> clearBridge()
            DesktopStatusAction.REPAIR_BRIDGE -> {
                session.clearIssue()
                reconnectBridge()
            }
        }
    }

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

    /** The menu bar / tray icon is easy to miss, so say where the window went — once. */
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

    fun enterCalibrationUi() {
        _calibrationUiActive.value = true
        ensureModelReady()
        restartCameraPipeline()
    }

    fun exitCalibrationUi() {
        if (_calibrationUiActive.value) {
            session.cancelCalibration()
        }
        _calibrationUiActive.value = false
        if (!_showPreview.value) {
            _previewBitmap.value = null
        }
        if (!needsCamera()) stopCameraPipeline()
    }

    fun setShowPreview(value: Boolean) {
        prefs.putBoolean("show_preview", value)
        _showPreview.value = value
        if (value) {
            startCameraPipeline()
        } else {
            _previewBitmap.value = null
            if (!needsCamera()) stopCameraPipeline()
        }
    }

    fun setLowPower(value: Boolean) {
        prefs.putBoolean("low_power", value)
        _lowPower.value = value
        lastTargetFps = if (value) 3 else 5
        if (needsCamera()) restartCameraPipeline()
    }

    fun setOpenAtLogin(value: Boolean) {
        val result = LoginItemManager.setEnabled(value)
        _openAtLogin.value = result.enabled
        _openAtLoginMessage.value = result.message
    }

    fun setSensitivity(level: SensitivityLevel) {
        if (session.uiState.value.settingsFromPhone) return
        prefs.put("sensitivity", level.name)
        session.updateSensitivity(level)
        session.currentCalibration()?.let { CalibrationStore.save(prefs, it) }
    }

    fun setSlumpDurationMs(ms: Long) {
        if (session.uiState.value.settingsFromPhone) return
        prefs.putLong("slump_duration_ms", ms)
        session.updateTimers(ms, prefs.getLong("repeat_alert_ms", 5_000L))
        session.currentCalibration()?.let { CalibrationStore.save(prefs, it) }
    }

    fun setRepeatAlertMs(ms: Long) {
        if (session.uiState.value.settingsFromPhone) return
        prefs.putLong("repeat_alert_ms", ms)
        session.updateTimers(prefs.getLong("slump_duration_ms", 30_000L), ms)
        session.currentCalibration()?.let { CalibrationStore.save(prefs, it) }
    }

    fun selectCamera(deviceId: String) {
        ensureCamera()
        (camera as? JvmCameraFrameSource)?.selectDevice(deviceId, lastTargetFps)
            ?: camera?.selectDevice(deviceId)
        prefs.put("camera_id", deviceId)
    }

    fun refreshCameras() {
        ensureCamera()
        camera?.refreshDevices()
        val err = camera?.lastError?.value
        if (err != null) {
            session.onCameraError(err)
        } else {
            session.clearIssue()
        }
    }

    fun retryCamera() {
        session.clearIssue()
        stopCameraPipeline()
        ensureCamera()
        camera?.refreshDevices()
        val err = camera?.lastError?.value
        if (err != null) {
            session.onCameraError(err)
            return
        }
        if (needsCamera()) {
            startCameraPipeline()
        }
    }

    fun startSession() {
        if (!ensureModelReady()) return
        if (!session.startSession()) return
        startCameraPipeline()
        bridgeStore.emitSessionEvent(DesktopSlumpEventType.SESSION_STARTED)
    }

    fun stopSession() {
        session.stopSession()
        if (!needsCamera()) stopCameraPipeline()
        bridgeStore.emitSessionEvent(DesktopSlumpEventType.SESSION_STOPPED)
    }

    fun beginErectCalibration() {
        if (!ensureModelReady()) return
        if (!session.beginErectCalibration()) return
        startCameraPipeline()
    }

    fun beginSlumpCalibration() {
        if (!ensureModelReady()) return
        if (!session.beginSlumpCalibration()) return
        startCameraPipeline()
    }

    fun cancelCalibration() {
        session.cancelCalibration()
        if (!needsCamera()) stopCameraPipeline()
    }

    fun pairPhone(host: String, code: String, onResult: (String) -> Unit) =
        bridgeStore.pairPhone(host, code, onResult)

    fun showPairQr() = bridgeStore.showPairQr()

    fun cancelPairQr() = bridgeStore.cancelPairQr()

    fun clearBridge() = bridgeStore.clearBridge()

    fun shutdown() {
        bridgeStore.shutdown()
        stopSession()
        poseEstimator?.close()
        poseEstimator = null
        (camera as? JvmCameraFrameSource)?.dispose()
        camera = null
        alerter.dispose()
        scope.cancel()
    }

    private fun ensureModelReady(): Boolean {
        ensurePose()
        if (!modelBytesLoaded) {
            session.setModelReady(false)
            return false
        }
        session.setModelReady(true)
        return true
    }

    // The warm-up runs in the background while the UI can still trigger these, so both entry
    // points share a lock to avoid creating two cameras or two ONNX sessions.
    private fun ensureCamera() {
        synchronized(visionLock) {
            if (!prefs.getBoolean("camera_consent_accepted", false)) return
            if (camera == null) {
                camera = VisionPlatform.createCameraFrameSource()
                val saved = prefs.get("camera_id", null)
                if (saved != null) {
                    (camera as? JvmCameraFrameSource)?.selectDevice(saved, lastTargetFps)
                        ?: camera?.selectDevice(saved)
                }
                // Surface empty-device / permission state immediately on Mac.
                camera?.refreshDevices()
                camera?.lastError?.value?.let { session.onCameraError(it) }
            }
        }
    }

    private fun ensurePose() = synchronized(visionLock) {
        if (poseEstimator == null) {
            val bytes = loadModelBytes()
            modelBytesLoaded = bytes != null
            poseEstimator = VisionPlatform.createPoseEstimator(bytes)
            session.setModelReady(modelBytesLoaded)
        }
    }

    private fun needsCamera(): Boolean {
        if (_showPreview.value || _calibrationUiActive.value) return true
        val ui = session.uiState.value
        return ui.phase != DesktopSessionPhase.IDLE ||
            ui.calibrationPhase == CalibrationPhase.CAPTURE_ERECT ||
            ui.calibrationPhase == CalibrationPhase.CAPTURE_SLUMP
    }

    private fun restartCameraPipeline() {
        stopCameraPipeline()
        if (needsCamera()) startCameraPipeline()
    }

    private fun startCameraPipeline() {
        ensurePose()
        if (pipelineJob?.isActive == true) return
        ensureCamera()
        val cam = camera ?: return
        val estimator = poseEstimator
        val wantPose = estimator != null && estimator !is MissingModelPoseEstimator
        if (!wantPose && !_showPreview.value) {
            session.setModelReady(false)
            return
        }
        if (!wantPose) {
            session.setModelReady(false)
        } else {
            session.setModelReady(true)
        }
        lastTargetFps = if (_lowPower.value) 3 else 5
        cam.start(targetFps = lastTargetFps)
        pipelineJob = scope.launch {
            launch {
                cam.lastError.collectLatest { err ->
                    if (err != null) {
                        session.onCameraError(err)
                    } else {
                        session.onCameraRecovered()
                    }
                }
            }
            cam.frames.collect { frame ->
                if (_showPreview.value || _calibrationUiActive.value) {
                    val maxW = if (_calibrationUiActive.value) 1280 else 480
                    _previewBitmap.value = withContext(Dispatchers.Default) {
                        FramePreview.toImageBitmap(frame, maxWidth = maxW)
                    }
                }
                if (wantPose) {
                    val pose = withContext(Dispatchers.Default) {
                        estimator!!.estimate(frame)
                    }
                    // Wall clock — frame timestamps must not stall calibration.
                    session.onPose(pose, System.currentTimeMillis())
                }
            }
        }
    }

    private fun stopCameraPipeline() {
        pipelineJob?.cancel()
        pipelineJob = null
        camera?.stop()
        if (!_showPreview.value) {
            _previewBitmap.value = null
        }
    }

    private fun loadModelBytes(): ByteArray? {
        val resource = javaClass.getResourceAsStream("/models/movenet_lightning.onnx")
        if (resource != null) return resource.use { it.readBytes() }
        val file = java.io.File("desktopApp/src/main/resources/models/movenet_lightning.onnx")
        if (file.isFile) return file.readBytes()
        val local = java.io.File(System.getProperty("user.home"), ".keepstraight/movenet_lightning.onnx")
        if (local.isFile) return local.readBytes()
        return null
    }
}
