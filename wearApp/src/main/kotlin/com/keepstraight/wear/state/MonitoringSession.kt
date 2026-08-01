package com.keepstraight.wear.state

import android.content.Context
import com.keepstraight.shared.domain.AnalyzerResult
import com.keepstraight.shared.domain.PostureMonitoringEngine
import com.keepstraight.shared.model.AlertPreferences
import com.keepstraight.shared.model.CalibrationCaptureResult
import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.model.PostureEvent
import com.keepstraight.shared.model.WatchControlMessage
import com.keepstraight.shared.presentation.MonitoringState
import com.keepstraight.wear.R
import com.keepstraight.wear.platform.AndroidDoNotDisturbChecker
import com.keepstraight.wear.service.PostureMonitoringService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MonitoringSession(context: Context) {

    private val appContext = context
    private val engine = PostureMonitoringEngine(AndroidDoNotDisturbChecker(appContext))

    private val _statusText = MutableStateFlow(appContext.getString(R.string.status_algorithm_off))
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    val monitoringState: StateFlow<MonitoringState> = engine.monitoringState

    var onAlert: ((AnalyzerResult) -> Unit)?
        get() = engine.onAlert
        set(value) {
            engine.onAlert = value
        }

    var onPostureEvent: ((PostureEvent) -> Unit)?
        get() = engine.onPostureEvent
        set(value) {
            engine.onPostureEvent = value
        }

    var onCalibrationComplete: ((CalibrationCaptureResult) -> Unit)?
        get() = engine.onCalibrationComplete
        set(value) {
            engine.onCalibrationComplete = value
        }

    var onSyncRequested: (() -> Unit)?
        get() = engine.onSyncRequested
        set(value) {
            engine.onSyncRequested = value
        }

    var onConnectionRetry: (() -> Unit)? = null

    var onConnectionRetryExhausted: (() -> Unit)? = null

    private var externalStateChanged: (() -> Unit)? = null

    var onStateChanged: (() -> Unit)?
        get() = externalStateChanged
        set(value) {
            externalStateChanged = value
        }

    init {
        engine.onStartMonitoring = {
            PostureMonitoringService.setMonitoringEnabled(appContext, true)
            PostureMonitoringService.start(appContext)
        }
        engine.onStopMonitoring = {
            PostureMonitoringService.setMonitoringEnabled(appContext, false)
            PostureMonitoringService.stop(appContext)
        }
        engine.onCancelRetryCycle = {
            PostureMonitoringService.cancelRetryCycle(appContext)
        }
        engine.onStateChanged = {
            _statusText.value = statusLabelFor(appContext, engine.monitoringState.value)
            externalStateChanged?.invoke()
        }
    }

    fun updateConfig(newConfig: PostureCalibrationConfig) = engine.updateConfig(newConfig)

    fun updateAlertPreferences(preferences: AlertPreferences) = engine.updateAlertPreferences(preferences)

    fun getAlertPreferences(): AlertPreferences = engine.getAlertPreferences()

    fun handleControlMessage(message: WatchControlMessage) = engine.handleControlMessage(message)

    fun startMonitoring() = engine.startMonitoring()

    fun stopMonitoring() = engine.stopMonitoring()

    fun startCalibrationCapture() = engine.startCalibrationCapture()

    fun setPhoneRetryActive(active: Boolean) = engine.setPhoneRetryActive(active)

    fun setPhoneDisconnectedPaused() = engine.setPhoneDisconnectedPaused()

    fun handleConnectionRetry() {
        PostureMonitoringService.handleRetryAlarm(appContext)
    }

    fun processSample(
        ax: Float,
        ay: Float,
        az: Float,
        stepCount: Int,
        offWrist: Boolean,
        timestampMs: Long = System.currentTimeMillis(),
    ) = engine.processSample(ax, ay, az, stepCount, offWrist, timestampMs)

    private fun statusLabelFor(context: Context, state: MonitoringState): String = when (state) {
        MonitoringState.ACTIVE -> context.getString(R.string.status_monitoring)
        MonitoringState.ALERTS_PAUSED -> context.getString(R.string.status_alerts_paused)
        MonitoringState.ALGORITHM_OFF -> context.getString(R.string.status_algorithm_off)
        MonitoringState.NOT_SITTING -> context.getString(R.string.status_not_sitting)
        MonitoringState.NOT_WORN -> context.getString(R.string.status_not_worn)
        MonitoringState.PHONE_RETRY -> context.getString(R.string.status_phone_retry)
        MonitoringState.PHONE_DISCONNECTED_PAUSED -> context.getString(R.string.status_phone_paused)
        MonitoringState.DND_ACTIVE -> context.getString(R.string.status_dnd)
    }
}
