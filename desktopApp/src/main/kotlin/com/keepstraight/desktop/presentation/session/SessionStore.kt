package com.keepstraight.desktop.presentation.session

import com.keepstraight.desktop.CalibrationStore
import com.keepstraight.desktop.alert.DesktopAlerter
import com.keepstraight.desktop.presentation.bridge.BridgeStore
import com.keepstraight.desktop.presentation.camera.CameraStore
import com.keepstraight.shared.bridge.DesktopSlumpEventType
import com.keepstraight.shared.domain.CalibrationPhase
import com.keepstraight.shared.domain.DesktopPostureSession
import com.keepstraight.shared.domain.DesktopSessionPhase
import com.keepstraight.desktop.presentation.DesktopPrefsKeys
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.presentation.DesktopStatusAction
import com.keepstraight.shared.presentation.DesktopStatusMapper
import com.keepstraight.shared.presentation.DesktopStatusPresentation
import com.keepstraight.shared.presentation.StatusCopyKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.prefs.Preferences

class SessionStore(
    private val prefs: Preferences,
    private val scope: CoroutineScope,
    val session: DesktopPostureSession,
    private val alerter: DesktopAlerter,
    private val bridgeStore: BridgeStore,
    private val cameraStore: CameraStore,
) {
    val uiState = session.uiState

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
            statusKey = ui.statusKey,
            statusArgs = ui.statusArgs,
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
            statusKey = StatusCopyKey.BODY_START_SESSION,
            statusArgs = emptyList(),
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
    }

    fun applyLocalSettings() {
        val sensitivity = runCatching {
            SensitivityLevel.valueOf(prefs.get(DesktopPrefsKeys.SENSITIVITY, SensitivityLevel.NORMAL.name))
        }.getOrDefault(SensitivityLevel.NORMAL)
        session.updateSensitivity(sensitivity)
        session.updateTimers(
            slumpDurationThresholdMs = prefs.getLong(DesktopPrefsKeys.SLUMP_DURATION_MS, 30_000L),
            repeatAlertIntervalMs = prefs.getLong(DesktopPrefsKeys.REPEAT_ALERT_MS, 5_000L),
        )
    }

    fun handleStatusAction(action: DesktopStatusAction) {
        when (action) {
            DesktopStatusAction.RETRY_CAMERA -> cameraStore.retryCamera()
            DesktopStatusAction.REFRESH_CAMERAS -> cameraStore.refreshCameras()
            DesktopStatusAction.CALIBRATE,
            DesktopStatusAction.CALIBRATE_ERECT,
            DesktopStatusAction.CALIBRATE_SLUMP,
            -> Unit
            DesktopStatusAction.STOP_SESSION -> stopSession()
            DesktopStatusAction.CLEAR_BRIDGE -> bridgeStore.clearBridge()
            DesktopStatusAction.REPAIR_BRIDGE -> {
                session.clearIssue()
                bridgeStore.reconnectBridge()
            }
        }
    }

    fun setSensitivity(level: SensitivityLevel) {
        if (session.uiState.value.settingsFromPhone) return
        prefs.put(DesktopPrefsKeys.SENSITIVITY, level.name)
        session.updateSensitivity(level)
        session.currentCalibration()?.let { CalibrationStore.save(prefs, it) }
    }

    fun setSlumpDurationMs(ms: Long) {
        if (session.uiState.value.settingsFromPhone) return
        prefs.putLong(DesktopPrefsKeys.SLUMP_DURATION_MS, ms)
        session.updateTimers(ms, prefs.getLong(DesktopPrefsKeys.REPEAT_ALERT_MS, 5_000L))
        session.currentCalibration()?.let { CalibrationStore.save(prefs, it) }
    }

    fun setRepeatAlertMs(ms: Long) {
        if (session.uiState.value.settingsFromPhone) return
        prefs.putLong(DesktopPrefsKeys.REPEAT_ALERT_MS, ms)
        session.updateTimers(prefs.getLong(DesktopPrefsKeys.SLUMP_DURATION_MS, 30_000L), ms)
        session.currentCalibration()?.let { CalibrationStore.save(prefs, it) }
    }

    fun startSession() {
        if (!cameraStore.ensureModelReady()) return
        if (!session.startSession()) return
        cameraStore.startCameraPipeline()
        bridgeStore.emitSessionEvent(DesktopSlumpEventType.SESSION_STARTED)
    }

    fun stopSession() {
        session.stopSession()
        if (!cameraStore.needsCamera()) cameraStore.stopCameraPipeline()
        bridgeStore.emitSessionEvent(DesktopSlumpEventType.SESSION_STOPPED)
    }

    fun beginErectCalibration() {
        if (!cameraStore.ensureModelReady()) return
        if (!session.beginErectCalibration()) return
        cameraStore.startCameraPipeline()
    }

    fun beginSlumpCalibration() {
        if (!cameraStore.ensureModelReady()) return
        if (!session.beginSlumpCalibration()) return
        cameraStore.startCameraPipeline()
    }

    fun cancelCalibration() {
        session.cancelCalibration()
        if (!cameraStore.needsCamera()) cameraStore.stopCameraPipeline()
    }

    fun enterCalibrationUi() {
        cameraStore.enterCalibrationUi()
    }

    fun exitCalibrationUi() {
        if (cameraStore.calibrationUiActive.value) {
            session.cancelCalibration()
        }
        cameraStore.onExitCalibrationUi()
    }

    fun shutdown() {
        alerter.dispose()
    }
}
