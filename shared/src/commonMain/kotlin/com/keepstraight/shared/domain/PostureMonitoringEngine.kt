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
    private var calibrationStartedAt = 0L
    private var lastActivityState = ActivityState.AMBIGUOUS

    private val _monitoringState = MutableStateFlow(MonitoringState.ALGORITHM_OFF)
    val monitoringState: StateFlow<MonitoringState> = _monitoringState.asStateFlow()

    var onAlert: ((AnalyzerResult) -> Unit)? = null
    var onPostureEvent: ((PostureEvent) -> Unit)? = null
    var onCalibrationComplete: ((CalibrationCaptureResult) -> Unit)? = null
    var onSyncRequested: (() -> Unit)? = null
    var onStartMonitoring: (() -> Unit)? = null
    var onStopMonitoring: (() -> Unit)? = null
    var onCancelRetryCycle: (() -> Unit)? = null
    var onStateChanged: (() -> Unit)? = null

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
                onStartMonitoring?.invoke()
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

    fun stopMonitoring() {
        algorithmEnabled = false
        postureAnalyzer.resetState()
        onStopMonitoring?.invoke()
        refreshDerivedState()
    }

    fun startCalibrationCapture(timestampMs: Long = currentTimeMillis()) {
        calibrationCapturing = true
        calibrationSamples.clear()
        calibrationStartedAt = timestampMs
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
            calibrationSamples.add(pitch to roll)
            if (timestampMs - calibrationStartedAt >= CALIBRATION_CAPTURE_MS) {
                finishCalibrationCapture(timestampMs)
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

        val activityState = activityClassifier.classify(pitch, roll, stepCount, timestampMs)
        lastActivityState = activityState
        updateDerivedState(activityState)

        if (activityState != ActivityState.SITTING || !canAnalyzePosture()) {
            postureAnalyzer.resetState()
            return
        }

        val result = postureAnalyzer.processSample(pitch, roll, activityState, timestampMs)
        when (result) {
            AnalyzerResult.SLUMP_INITIAL_ALERT -> {
                if (!alertsPaused && !dndChecker.isActive()) {
                    onAlert?.invoke(result)
                }
                emitEvent(
                    PostureEventType.SLUMP_DETECTED,
                    postureAnalyzer.slumpDurationSeconds(timestampMs),
                    timestampMs,
                )
            }
            AnalyzerResult.SLUMP_REPEAT_ALERT -> {
                if (!alertsPaused && !dndChecker.isActive()) {
                    onAlert?.invoke(result)
                }
            }
            AnalyzerResult.POSTURE_CORRECTED,
            AnalyzerResult.STATE_RESET,
            -> postureAnalyzer.resetState()
            AnalyzerResult.NONE -> Unit
        }
    }

    private fun resumeAfterDisconnect() {
        phoneDisconnectedPaused = false
        phoneRetryActive = false
        onCancelRetryCycle?.invoke()
        startMonitoring()
        onSyncRequested?.invoke()
    }

    private fun canAnalyzePosture(): Boolean = when (_monitoringState.value) {
        MonitoringState.ACTIVE,
        MonitoringState.ALERTS_PAUSED,
        MonitoringState.PHONE_RETRY,
        MonitoringState.DND_ACTIVE,
        -> true
        else -> false
    }

    private fun finishCalibrationCapture(timestampMs: Long) {
        calibrationCapturing = false
        if (calibrationSamples.isEmpty()) return

        val avgPitch = calibrationSamples.map { it.first }.average().toFloat()
        val avgRoll = calibrationSamples.map { it.second }.average().toFloat()
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
            !algorithmEnabled || config == null -> MonitoringState.ALGORITHM_OFF
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

