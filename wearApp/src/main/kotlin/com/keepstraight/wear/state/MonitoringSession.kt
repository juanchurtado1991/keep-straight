package com.keepstraight.wear.state

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.keepstraight.shared.domain.AnalyzerResult
import com.keepstraight.shared.domain.PostureMonitoringEngine
import com.keepstraight.shared.model.AlertPreferences
import com.keepstraight.shared.model.CalibrationCaptureResult
import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.model.PostureEvent
import com.keepstraight.shared.model.WatchControlCommand
import com.keepstraight.shared.model.WatchControlMessage
import com.keepstraight.shared.presentation.MonitoringState
import com.keepstraight.shared.sync.CalibrationResultCodec
import com.keepstraight.shared.sync.SyncPaths
import com.keepstraight.wear.R
import com.keepstraight.wear.alerts.AlertDispatcher
import com.keepstraight.wear.platform.AndroidDoNotDisturbChecker
import com.keepstraight.wear.sensors.CalibrationSensorSampler
import com.keepstraight.wear.service.PostureMonitoringService
import com.keepstraight.wear.sync.WearMessageSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MonitoringSession(context: Context) {

    private val appContext = context.applicationContext
    private val engine = PostureMonitoringEngine(AndroidDoNotDisturbChecker(appContext))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val messageSender = WearMessageSender(appContext)
    private val dataClient by lazy { Wearable.getDataClient(appContext) }

    val monitoringState: StateFlow<MonitoringState> = engine.monitoringState
    val isCalibrating: StateFlow<Boolean> = engine.isCalibrating
    val liveDeviationDegrees: StateFlow<Float> = engine.liveDeviationDegrees
    val liveSlumpScore: StateFlow<Float> = engine.liveSlumpScore

    private val _statusText = kotlinx.coroutines.flow.MutableStateFlow(
        appContext.getString(R.string.status_algorithm_off),
    )
    val statusText: StateFlow<String> = _statusText

    @Volatile
    private var calibrationReplyNodeId: String? = null

    private val calibrationSampler = CalibrationSensorSampler(appContext) { ax, ay, az, timestampMs ->
        engine.processSample(
            ax = ax,
            ay = ay,
            az = az,
            stepCount = 0,
            offWrist = false,
            timestampMs = timestampMs,
        )
    }

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

    var onSyncRequested: (() -> Unit)?
        get() = engine.onSyncRequested
        set(value) {
            engine.onSyncRequested = value
        }

    var onConnectionRetry: (() -> Unit)? = null

    var onConnectionRetryExhausted: (() -> Unit)? = null

    var onEnsureSensors: (() -> Unit)?
        get() = engine.onEnsureSensors
        set(value) {
            engine.onEnsureSensors = value
        }

    private var externalStateChanged: (() -> Unit)? = null

    var onStateChanged: (() -> Unit)?
        get() = externalStateChanged
        set(value) {
            externalStateChanged = value
        }

    init {
        // Desktop → phone → TRIGGER_ALERT must vibrate even when the monitoring FGS
        // was never started (wrist slump path is disabled).
        val alertDispatcher = AlertDispatcher(appContext)
        engine.onAlert = { alertDispatcher.dispatchAlert(engine.getAlertPreferences()) }

        engine.onStartMonitoring = {
            PostureMonitoringService.setMonitoringEnabled(appContext, true)
            runCatching { PostureMonitoringService.start(appContext) }
        }
        engine.onStopMonitoring = {
            PostureMonitoringService.setMonitoringEnabled(appContext, false)
            runCatching { PostureMonitoringService.stop(appContext) }
        }
        engine.onCancelRetryCycle = {
            PostureMonitoringService.cancelRetryCycle(appContext)
        }
        engine.onStateChanged = {
            _statusText.value = if (engine.isCalibrating.value) {
                appContext.getString(R.string.status_calibrating)
            } else {
                statusLabelFor(appContext, engine.monitoringState.value)
            }
            if (!engine.isCalibrating.value) {
                runCatching { calibrationSampler.stop() }
            }
            externalStateChanged?.invoke()
        }
        engine.onCalibrationComplete = { result ->
            runCatching { calibrationSampler.stop() }
            sendCalibrationResult(result)
        }

        // Default: ready for desktop→phone→watch haptics (no wrist FGS).
        engine.enableDesktopAlertMode()
    }

    fun updateConfig(newConfig: PostureCalibrationConfig) = engine.updateConfig(newConfig)

    fun updateAlertPreferences(preferences: AlertPreferences) = engine.updateAlertPreferences(preferences)

    fun getAlertPreferences(): AlertPreferences = engine.getAlertPreferences()

    fun handleControlMessage(message: WatchControlMessage) = engine.handleControlMessage(message)

    fun beginCalibrationFromPhone(phoneNodeId: String) {
        if (engine.isCalibrating.value) {
            Log.i(TAG, "Already calibrating; updating reply node=$phoneNodeId")
            calibrationReplyNodeId = phoneNodeId
            calibrationSampler.start()
            return
        }
        Log.i(TAG, "Calibration requested from phone node=$phoneNodeId")
        calibrationReplyNodeId = phoneNodeId
        PostureMonitoringService.setMonitoringEnabled(appContext, true)
        runCatching { PostureMonitoringService.start(appContext) }
            .onFailure { Log.w(TAG, "FGS start failed; continuing with direct sampler", it) }

        engine.handleControlMessage(WatchControlMessage(WatchControlCommand.CALIBRATE_CAPTURE))
        calibrationSampler.start()
    }

    fun startMonitoring() = engine.startMonitoring()

    fun enableDesktopAlertMode() = engine.enableDesktopAlertMode()

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

    private fun sendCalibrationResult(result: CalibrationCaptureResult) {
        val replyTo = calibrationReplyNodeId
        calibrationReplyNodeId = null
        scope.launch {
            val bytes = CalibrationResultCodec.encode(result)
            Log.i(TAG, "Sending CALIBRATE_RESULT pitch=${result.basePitch} roll=${result.baseRoll}")

            var sent = false
            if (replyTo != null) {
                sent = messageSender.sendToNode(replyTo, SyncPaths.CALIBRATE_RESULT, bytes)
            }
            if (!sent) {
                sent = messageSender.sendToPhone(SyncPaths.CALIBRATE_RESULT, bytes)
            }
            if (!sent) {
                delay(500)
                sent = messageSender.sendToPhone(SyncPaths.CALIBRATE_RESULT, bytes)
            }

            runCatching {
                val request = PutDataMapRequest.create(SyncPaths.CALIBRATE_RESULT).apply {
                    dataMap.putFloat(CalibrationResultCodec.KEY_PITCH, result.basePitch)
                    dataMap.putFloat(CalibrationResultCodec.KEY_ROLL, result.baseRoll)
                    dataMap.putLong(CalibrationResultCodec.KEY_CAPTURED_AT, result.capturedAt)
                    dataMap.putLong(CalibrationResultCodec.KEY_SENT_AT, System.currentTimeMillis())
                }.asPutDataRequest().setUrgent()
                dataClient.putDataItem(request).await()
                Log.i(TAG, "CALIBRATE_RESULT published on Data Layer")
            }.onFailure {
                Log.e(TAG, "Data Layer publish failed", it)
            }

            if (!sent) {
                Log.e(TAG, "Message API failed to deliver CALIBRATE_RESULT")
            }
        }
    }

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

    companion object {
        private const val TAG = "KeepStraightWear"
    }
}
