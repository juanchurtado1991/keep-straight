package com.keepstraight.shared.domain

import com.keepstraight.shared.model.AlertPreferences
import com.keepstraight.shared.model.CalibrationCaptureResult
import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.model.PostureEvent
import com.keepstraight.shared.model.PostureEventType
import com.keepstraight.shared.model.WatchControlCommand
import com.keepstraight.shared.model.WatchControlMessage
import com.keepstraight.shared.platform.DoNotDisturbChecker
import com.keepstraight.shared.platform.currentTimeMillis
import com.keepstraight.shared.presentation.MonitoringState
import com.keepstraight.shared.sync.SyncTiming
import com.keepstraight.shared.util.ImuMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PostureMonitoringEngine(
    private val dndChecker: DoNotDisturbChecker,
) {
    private val postureAnalyzer = PostureAnalyzer(null)
    private val activityClassifier = ActivityClassifier(null)

    private var config: PostureCalibrationConfig? = null
    private var alertPreferences = AlertPreferences()
    private var alertsPaused = false
    private var algorithmEnabled = false
    private var phoneDisconnectedPaused = false
    private var phoneRetryActive = false

    private var calibrationCapturing = false
    private var calibrationSamples = mutableListOf<Pair<Float, Float>>()
    private var calibrationStartedAt = -1L
    private var calibrationRequestAt = 0L
    private var lastActivityState = ActivityState.AMBIGUOUS

    private val _monitoringState = MutableStateFlow(MonitoringState.ALGORITHM_OFF)
    val monitoringState: StateFlow<MonitoringState> = _monitoringState.asStateFlow()

    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating: StateFlow<Boolean> = _isCalibrating.asStateFlow()

    private val _liveDeviationDegrees = MutableStateFlow(0f)
    /** Max pitch/roll delta from good baseline — for watch UI feedback. */
    val liveDeviationDegrees: StateFlow<Float> = _liveDeviationDegrees.asStateFlow()

    private val _liveSlumpScore = MutableStateFlow(0f)
    /** 0 = good, 1 = at slouch reference when dual-pose calibration is set. */
    val liveSlumpScore: StateFlow<Float> = _liveSlumpScore.asStateFlow()

    var onAlert: ((AnalyzerResult) -> Unit)? = null
    var onPostureEvent: ((PostureEvent) -> Unit)? = null
    var onCalibrationComplete: ((CalibrationCaptureResult) -> Unit)? = null
    var onSyncRequested: (() -> Unit)? = null
    var onStartMonitoring: (() -> Unit)? = null
    var onStopMonitoring: (() -> Unit)? = null
    var onCancelRetryCycle: (() -> Unit)? = null
    var onStateChanged: (() -> Unit)? = null
    /** Force accelerometer/step listeners on (e.g. during calibration). */
    var onEnsureSensors: (() -> Unit)? = null

    fun updateConfig(newConfig: PostureCalibrationConfig) {
        config = newConfig
        postureAnalyzer.updateConfig(newConfig)
        activityClassifier.updateConfig(newConfig)
        if (!algorithmEnabled) {
            startMonitoring()
        }
        refreshDerivedState()
    }

    fun updateAlertPreferences(preferences: AlertPreferences) {
        alertPreferences = preferences
    }

    fun getAlertPreferences(): AlertPreferences = alertPreferences

    fun handleControlMessage(message: WatchControlMessage) {
        when (message.command) {
            WatchControlCommand.TRIGGER_ALERT -> {
                if (!alertsPaused && !dndChecker.isActive()) {
                    onAlert?.invoke(AnalyzerResult.SLUMP_INITIAL_ALERT)
                }
            }
            WatchControlCommand.PAUSE_ALERTS -> {
                alertsPaused = true
                emitEvent(PostureEventType.MONITORING_PAUSED)
            }
            WatchControlCommand.RESUME_ALERTS -> {
                alertsPaused = false
                emitEvent(PostureEventType.MONITORING_RESUMED)
            }
            WatchControlCommand.STOP_ALGORITHM -> stopMonitoring()
            WatchControlCommand.START_ALGORITHM -> startMonitoring()
            WatchControlCommand.CALIBRATE_CAPTURE -> {
                // Connected phone can calibrate even after a disconnect pause.
                phoneDisconnectedPaused = false
                phoneRetryActive = false
                algorithmEnabled = true
                onStartMonitoring?.invoke()
                onEnsureSensors?.invoke()
                startCalibrationCapture()
            }
            WatchControlCommand.RESUME_CONNECTION -> resumeAfterDisconnect()
            WatchControlCommand.SYNC_PREFERENCES -> onSyncRequested?.invoke()
        }
        refreshDerivedState()
    }

    fun startMonitoring() {
        algorithmEnabled = true
        phoneDisconnectedPaused = false
        onStartMonitoring?.invoke()
        refreshDerivedState()
    }

    /**
     * Watch is an alert peripheral for desktop — ready for TRIGGER_ALERT without
     * starting the wrist IMU foreground service or requiring wrist calibration.
     */
    fun enableDesktopAlertMode() {
        algorithmEnabled = true
        phoneDisconnectedPaused = false
        refreshDerivedState()
    }

    fun stopMonitoring() {
        algorithmEnabled = false
        postureAnalyzer.resetState()
        onStopMonitoring?.invoke()
        refreshDerivedState()
    }

    fun startCalibrationCapture(timestampMs: Long = currentTimeMillis()) {
        calibrationCapturing = true
        _isCalibrating.value = true
        calibrationSamples.clear()
        // Start the 3s window on the first IMU sample so a slow service start
        // does not finish with an empty buffer and silently drop the result.
        calibrationStartedAt = -1L
        calibrationRequestAt = timestampMs
        onStateChanged?.invoke()
    }

    fun setPhoneRetryActive(active: Boolean) {
        phoneRetryActive = active
        if (!active) {
            phoneDisconnectedPaused = false
        }
        refreshDerivedState()
    }

    fun setPhoneDisconnectedPaused() {
        phoneRetryActive = false
        phoneDisconnectedPaused = true
        onStopMonitoring?.invoke()
        refreshDerivedState()
    }

    fun processSample(
        ax: Float,
        ay: Float,
        az: Float,
        stepCount: Int,
        offWrist: Boolean,
        timestampMs: Long = currentTimeMillis(),
    ) {
        activityClassifier.setOffWrist(offWrist)
        val (pitch, roll) = ImuMath.pitchRollDegrees(ax, ay, az)

        if (calibrationCapturing) {
            if (calibrationStartedAt < 0L) {
                calibrationStartedAt = timestampMs
            }
            calibrationSamples.add(pitch to roll)
            val captureElapsed = timestampMs - calibrationStartedAt
            val requestElapsed = timestampMs - calibrationRequestAt
            when {
                captureElapsed >= CALIBRATION_CAPTURE_MS &&
                    calibrationSamples.size >= MIN_CALIBRATION_SAMPLES -> {
                    finishCalibrationCapture(timestampMs)
                }
                requestElapsed >= CALIBRATION_GIVE_UP_MS -> {
                    // No usable samples in time — clear flag; phone times out with an error.
                    calibrationCapturing = false
                    _isCalibrating.value = false
                    calibrationSamples.clear()
                    calibrationStartedAt = -1L
                    onStateChanged?.invoke()
                }
            }
        }

        if (!algorithmEnabled || config == null || phoneDisconnectedPaused) {
            updateDerivedState(
                when {
                    offWrist -> ActivityState.NOT_WORN
                    phoneDisconnectedPaused -> lastActivityState
                    else -> ActivityState.AMBIGUOUS
                },
            )
            return
        }

        val activityState = activityClassifier.classify(
            pitch = pitch,
            roll = roll,
            ax = ax,
            ay = ay,
            az = az,
            stepCount = stepCount,
            currentTimeMs = timestampMs,
        )
        lastActivityState = activityState
        updateDerivedState(activityState)

        // Wrist IMU is not the spine sensor anymore — desktop webcam owns slump detection.
        // Keep activity/off-wrist state for watch UI; do not alert from pitch/roll.
        _liveDeviationDegrees.value = 0f
        _liveSlumpScore.value = 0f
        postureAnalyzer.resetState()
    }

    private fun resumeAfterDisconnect() {
        phoneDisconnectedPaused = false
        phoneRetryActive = false
        onCancelRetryCycle?.invoke()
        startMonitoring()
        onSyncRequested?.invoke()
    }

    private fun finishCalibrationCapture(timestampMs: Long) {
        calibrationCapturing = false
        _isCalibrating.value = false
        if (calibrationSamples.size < MIN_CALIBRATION_SAMPLES) {
            calibrationSamples.clear()
            calibrationStartedAt = -1L
            onStateChanged?.invoke()
            return
        }

        val avgPitch = calibrationSamples.map { it.first }.average().toFloat()
        val avgRoll = calibrationSamples.map { it.second }.average().toFloat()
        calibrationSamples.clear()
        calibrationStartedAt = -1L
        val currentConfig = config

        val newConfig = if (currentConfig != null) {
            currentConfig.copy(basePitch = avgPitch, baseRoll = avgRoll)
        } else {
            PostureCalibrationConfig(basePitch = avgPitch, baseRoll = avgRoll)
        }

        updateConfig(newConfig)
        emitEvent(PostureEventType.CALIBRATED, 0, timestampMs)
        onCalibrationComplete?.invoke(
            CalibrationCaptureResult(
                basePitch = avgPitch,
                baseRoll = avgRoll,
                capturedAt = timestampMs,
            ),
        )
    }

    private fun emitEvent(
        type: PostureEventType,
        durationSeconds: Int = 0,
        timestampMs: Long = currentTimeMillis(),
    ) {
        onPostureEvent?.invoke(
            PostureEvent(
                eventType = type,
                durationSeconds = durationSeconds,
                timestamp = timestampMs,
            ),
        )
    }

    private fun refreshDerivedState() {
        updateDerivedState(lastActivityState)
    }

    private fun updateDerivedState(activityState: ActivityState) {
        val state = when {
            phoneDisconnectedPaused -> MonitoringState.PHONE_DISCONNECTED_PAUSED
            !algorithmEnabled -> MonitoringState.ALGORITHM_OFF
            // Wrist calibration is optional — desktop owns slump detection.
            // With algorithm on and no wrist config, watch is haptics-ready.
            config == null -> MonitoringState.ACTIVE
            activityState == ActivityState.NOT_WORN -> MonitoringState.NOT_WORN
            dndChecker.isActive() -> MonitoringState.DND_ACTIVE
            phoneRetryActive -> MonitoringState.PHONE_RETRY
            alertsPaused -> MonitoringState.ALERTS_PAUSED
            activityState == ActivityState.WALKING || activityState == ActivityState.STANDING ->
                MonitoringState.NOT_SITTING
            activityState == ActivityState.SITTING -> MonitoringState.ACTIVE
            else -> MonitoringState.NOT_SITTING
        }
        _monitoringState.value = state
        onStateChanged?.invoke()
    }
}

private const val CALIBRATION_CAPTURE_MS = 3_000L
private const val CALIBRATION_GIVE_UP_MS = SyncTiming.CALIBRATION_CAPTURE_TIMEOUT_MS
private const val MIN_CALIBRATION_SAMPLES = 4

