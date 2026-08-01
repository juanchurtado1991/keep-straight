package com.keepstraight.wear.sync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.ghost.serialization.Ghost
import com.keepstraight.shared.model.AlertPreferences
import com.keepstraight.shared.model.CalibrationCaptureResult
import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.model.WatchControlMessage
import com.keepstraight.shared.sync.SyncPaths
import com.keepstraight.wear.KeepStraightWearApp
import com.keepstraight.wear.service.PostureMonitoringService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WearSyncListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var messageSender: WearMessageSender

    override fun onCreate() {
        super.onCreate()
        messageSender = WearMessageSender(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val app = application as KeepStraightWearApp
        val session = app.monitoringSession

        try {
            when (messageEvent.path) {
                SyncPaths.CALIBRATION -> {
                    val config = Ghost.deserialize<PostureCalibrationConfig>(messageEvent.data)
                    PostureMonitoringService.start(this)
                    session.updateConfig(config)
                }

                SyncPaths.CONTROL -> {
                    val control = Ghost.deserialize<WatchControlMessage>(messageEvent.data)
                    session.handleControlMessage(control)
                }

                SyncPaths.PREFERENCES -> {
                    val preferences = Ghost.deserialize<AlertPreferences>(messageEvent.data)
                    session.updateAlertPreferences(preferences)
                }

                SyncPaths.CALIBRATE_REQUEST -> {
                    PostureMonitoringService.start(this)
                    session.startCalibrationCapture()
                }

                SyncPaths.SYNC_REQUEST -> {
                    scope.launch { flushPendingEvents(messageEvent.sourceNodeId) }
                }

                SyncPaths.SYNC_ACK -> {
                    app.pendingSyncQueue.clear()
                    PostureMonitoringService.cancelRetryCycle(this)
                    session.setPhoneRetryActive(false)
                }
            }
        } catch (_: Exception) {
            // Ignore malformed payloads
        }
    }

    private suspend fun flushPendingEvents(nodeId: String) {
        val app = application as KeepStraightWearApp
        val batchBytes = app.pendingSyncQueue.encodeBatchBytes() ?: return
        val sent = messageSender.sendToNode(nodeId, SyncPaths.EVENTS_BATCH, batchBytes)
        if (sent) {
            app.pendingSyncQueue.clear()
            PostureMonitoringService.cancelRetryCycle(this)
            app.monitoringSession.setPhoneRetryActive(false)
        }
    }

    companion object {
        fun sendCalibrationResult(context: android.content.Context, result: CalibrationCaptureResult) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                val sender = WearMessageSender(context)
                val bytes = Ghost.encodeToBytes(result)
                sender.sendToPhone(SyncPaths.CALIBRATE_RESULT, bytes)
            }
        }
    }
}
