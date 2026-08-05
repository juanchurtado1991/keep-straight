package com.keepstraight.wear.sync

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.ghost.serialization.Ghost
import com.keepstraight.shared.model.AlertPreferences
import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.model.WatchControlCommand
import com.keepstraight.shared.model.WatchControlMessage
import com.keepstraight.shared.presentation.MonitoringState
import com.keepstraight.shared.sync.SyncCapabilities
import com.keepstraight.shared.sync.SyncPaths
import com.keepstraight.wear.KeepStraightWearApp
import com.keepstraight.wear.MainActivity
import com.keepstraight.wear.alerts.AlertDispatcher
import com.keepstraight.wear.service.PostureMonitoringService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Handles phone→watch messages while the wear process is alive.
 * Registered on [KeepStraightWearApp] so calibration works even when
 * Samsung MARs freezes [WearSyncListenerService].
 */
class WearInboundHandler(
    private val app: KeepStraightWearApp,
) : MessageClient.OnMessageReceivedListener,
    DataClient.OnDataChangedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val flushMutex = Mutex()
    private val recentDeliveryKeys = ArrayDeque<String>()
    private val deliveryLock = Any()
    private var lastCalibrateRequestAt = 0L
    private var lastDesktopAlertAt = 0L

    fun start() {
        val messageClient = Wearable.getMessageClient(app)
        val dataClient = Wearable.getDataClient(app)
        // Single listener each — avoid duplicate TRIGGER_ALERT delivery.
        messageClient.addListener(
            this,
            Uri.parse("wear://*/keepstraight"),
            MessageClient.FILTER_PREFIX,
        )
        dataClient.addListener(
            this,
            Uri.parse("wear://*/keepstraight"),
            DataClient.FILTER_PREFIX,
        )
        scope.launch {
            runCatching {
                Wearable.getCapabilityClient(app)
                    .addLocalCapability(SyncCapabilities.WEAR)
                    .await()
                Log.i(TAG, "Local capability ${SyncCapabilities.WEAR} advertised")
            }.onFailure { error ->
                if (error is com.google.android.gms.common.api.ApiException &&
                    error.statusCode == CAPABILITY_DUPLICATE
                ) {
                    Log.i(TAG, "Local capability ${SyncCapabilities.WEAR} already present")
                } else {
                    Log.w(TAG, "Capability advertise failed", error)
                }
            }
            logConnectedNodes()
        }
        Log.i(TAG, "Inbound listeners registered")
    }

    /** Called when the watch UI is visible — proves the Wear channel both ways. */
    fun onUiVisible() {
        scope.launch {
            logConnectedNodes()
            val sender = WearMessageSender(app)
            val nodes = sender.getConnectedNodes()
            Log.i(
                TAG,
                "UI visible; phone nodes=" +
                    nodes.joinToString { "${it.displayName}(${it.id},nearby=${it.isNearby})" },
            )
            val sent = sender.sendToPhone(
                SyncPaths.PING,
                "ui:${System.currentTimeMillis()}".encodeToByteArray(),
            )
            Log.i(TAG, "Ping → phone sent=$sent")
        }
    }

    private suspend fun logConnectedNodes() {
        val nodes = WearMessageSender(app).getConnectedNodes()
        Log.i(
            TAG,
            "Connected nodes=" +
                nodes.joinToString { "${it.displayName}(${it.id},nearby=${it.isNearby})" },
        )
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        handlePath(
            path = messageEvent.path,
            payload = messageEvent.data,
            sourceNodeId = messageEvent.sourceNodeId,
        )
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.use { buffer ->
            for (event in buffer) {
                if (event.type != DataEvent.TYPE_CHANGED) continue
                val path = event.dataItem.uri.path ?: continue
                if (path != SyncPaths.CALIBRATE_REQUEST) continue
                val phoneNodeId = event.dataItem.uri.host.orEmpty()
                Log.i(TAG, "DataItem calibrate-request from host=$phoneNodeId")
                handleCalibrateRequest(phoneNodeId)
            }
        }
    }

    fun handlePath(path: String, payload: ByteArray, sourceNodeId: String) {
        if (!acceptDelivery(path, payload, sourceNodeId)) {
            Log.i(TAG, "Skipping duplicate $path from $sourceNodeId")
            return
        }
        val session = app.monitoringSession
        Log.i(TAG, "Message $path from $sourceNodeId")

        try {
            when (path) {
                SyncPaths.CALIBRATION -> {
                    val config = Ghost.deserialize<PostureCalibrationConfig>(payload)
                    PostureMonitoringService.setMonitoringEnabled(app, true)
                    runCatching { PostureMonitoringService.start(app) }
                    session.updateConfig(config)
                }

                SyncPaths.CALIBRATE_REQUEST -> handleCalibrateRequest(sourceNodeId)

                SyncPaths.CONTROL -> {
                    val control = Ghost.deserialize<WatchControlMessage>(payload)
                    when (control.command) {
                        WatchControlCommand.CALIBRATE_CAPTURE ->
                            handleCalibrateRequest(sourceNodeId)
                        WatchControlCommand.TRIGGER_ALERT ->
                            handleDesktopTriggeredAlert()
                        else -> session.handleControlMessage(control)
                    }
                }

                SyncPaths.PREFERENCES -> {
                    val preferences = Ghost.deserialize<AlertPreferences>(payload)
                    session.updateAlertPreferences(preferences)
                }

                SyncPaths.SYNC_REQUEST -> {
                    scope.launch { flushPendingEvents(sourceNodeId) }
                }

                SyncPaths.SYNC_ACK -> {
                    PostureMonitoringService.cancelRetryCycle(app)
                    session.setPhoneRetryActive(false)
                }
            }
        } catch (error: Exception) {
            Log.e(TAG, "Failed handling $path", error)
        }
    }

    private fun handleCalibrateRequest(phoneNodeId: String) {
        val now = System.currentTimeMillis()
        if (now - lastCalibrateRequestAt < DEDUP_MS) {
            Log.i(TAG, "Ignoring duplicate calibrate-request")
            return
        }
        lastCalibrateRequestAt = now

        wakeForCalibration()
        app.monitoringSession.beginCalibrationFromPhone(phoneNodeId)
    }

    /**
     * Desktop already decided a slump alert fired — vibrate immediately without
     * requiring the wrist monitoring foreground service. Still honors the phone's
     * Alerts-paused toggle and haptic preference via [AlertDispatcher] (incl. DND).
     */
    private fun handleDesktopTriggeredAlert() {
        // ALGORITHM_OFF / NOT_WORN apply to the wrist IMU path. Desktop owns detection, so the
        // watch must still buzz when the algorithm was never started (default after install).
        val state = app.monitoringSession.monitoringState.value
        if (state == MonitoringState.ALERTS_PAUSED ||
            state == MonitoringState.NOT_WORN ||
            state == MonitoringState.PHONE_DISCONNECTED_PAUSED
        ) {
            Log.i(TAG, "Skipping desktop alert — state=$state")
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastDesktopAlertAt < DEDUP_MS) {
            Log.i(TAG, "Ignoring duplicate TRIGGER_ALERT")
            return
        }
        lastDesktopAlertAt = now
        wakeBriefly("keepstraight:desktop-alert", ALERT_WAKE_MS)
        val prefs = app.monitoringSession.getAlertPreferences()
        runCatching {
            AlertDispatcher(app).dispatchAlert(prefs)
            Log.i(TAG, "Desktop TRIGGER_ALERT dispatched haptic=${prefs.hapticEnabled} state=$state")
        }.onFailure { Log.e(TAG, "Desktop alert dispatch failed", it) }
    }

    private fun wakeForCalibration() {
        wakeBriefly("keepstraight:calibrate", WAKE_MS)
        runCatching {
            app.startActivity(
                Intent(app, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                },
            )
        }.onFailure { Log.w(TAG, "Could not bring MainActivity to front", it) }
    }

    private fun wakeBriefly(tag: String, durationMs: Long) {
        runCatching {
            val pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag)
            wakeLock.acquire(durationMs)
        }.onFailure { Log.w(TAG, "WakeLock failed ($tag)", it) }
    }

    fun retryPendingSyncFromAlarm() {
        scope.launch { flushPendingEventsToPhone() }
    }

    private suspend fun flushPendingEvents(nodeId: String) {
        flushMutex.withLock {
            val batch = app.pendingSyncQueue.takeBatch() ?: return
            val batchBytes = Ghost.encodeToBytes(batch)
            val sender = WearMessageSender(app)
            val sent = sender.sendToNode(nodeId, SyncPaths.EVENTS_BATCH, batchBytes)
            if (sent) {
                PostureMonitoringService.cancelRetryCycle(app)
                app.monitoringSession.setPhoneRetryActive(false)
            } else {
                app.pendingSyncQueue.reenqueue(batch)
            }
        }
    }

    private suspend fun flushPendingEventsToPhone() {
        flushMutex.withLock {
            val batch = app.pendingSyncQueue.takeBatch() ?: return
            val batchBytes = Ghost.encodeToBytes(batch)
            val sender = WearMessageSender(app)
            val sent = sender.sendToPhone(SyncPaths.EVENTS_BATCH, batchBytes)
            if (sent) {
                PostureMonitoringService.cancelRetryCycle(app)
                app.monitoringSession.setPhoneRetryActive(false)
            } else {
                app.pendingSyncQueue.reenqueue(batch)
            }
        }
    }

    private fun acceptDelivery(path: String, payload: ByteArray, sourceNodeId: String): Boolean {
        val key = "$path|$sourceNodeId|${payload.contentHashCode()}"
        synchronized(deliveryLock) {
            if (recentDeliveryKeys.contains(key)) return false
            recentDeliveryKeys.addLast(key)
            while (recentDeliveryKeys.size > MAX_RECENT_DELIVERIES) {
                recentDeliveryKeys.removeFirst()
            }
            return true
        }
    }

    private companion object {
        const val TAG = "KeepStraightWear"
        const val DEDUP_MS = 2_000L
        const val WAKE_MS = 20_000L
        const val ALERT_WAKE_MS = 3_000L
        const val CAPABILITY_DUPLICATE = 4006
        const val MAX_RECENT_DELIVERIES = 32
    }
}
