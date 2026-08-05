package com.keepstraight.sync

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.ghost.serialization.Ghost
import com.keepstraight.KeepStraightApp
import com.keepstraight.shared.model.PostureEvent
import com.keepstraight.shared.model.PostureEventBatch
import com.keepstraight.shared.model.PostureEventType
import com.keepstraight.shared.sync.CalibrationResultCodec
import com.keepstraight.shared.sync.SyncPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PhoneSyncListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val app: KeepStraightApp
        get() = application as KeepStraightApp

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.i(TAG, "Service message ${messageEvent.path} from ${messageEvent.sourceNodeId}")
        serviceScope.launch {
            when (messageEvent.path) {
                SyncPaths.PING -> {
                    Log.i(TAG, "Ping from watch (service) payload=${messageEvent.data.decodeToString()}")
                }
                SyncPaths.EVENTS -> handleEvent(messageEvent.data)
                SyncPaths.EVENTS_BATCH -> handleEventBatch(messageEvent.data)
                SyncPaths.CALIBRATE_RESULT -> handleCalibrateResult(messageEvent.data)
            }
        }
    }

    private suspend fun handleEvent(data: ByteArray) {
        runCatching {
            val event: PostureEvent = Ghost.deserialize(data)
            app.postureHistoryRepository.insertEvent(event)
            if (event.eventType == PostureEventType.SLUMP_DETECTED) {
                maybeNotifySlump(event)
            }
        }.onFailure { Log.e(TAG, "Failed handling event", it) }
    }

    private suspend fun handleEventBatch(data: ByteArray) {
        runCatching {
            val batch: PostureEventBatch = Ghost.deserialize(data)
            app.postureHistoryRepository.insertEvents(batch.events)
            batch.events
                .filter { it.eventType == PostureEventType.SLUMP_DETECTED }
                .maxByOrNull { it.timestamp }
                ?.let { maybeNotifySlump(it) }
        }.onFailure { Log.e(TAG, "Failed handling event batch", it) }
    }

    private suspend fun handleCalibrateResult(data: ByteArray) {
        runCatching {
            val result = CalibrationResultCodec.decode(data)
            app.userPreferencesRepository.setCalibration(result.basePitch, result.baseRoll)
        }.onFailure { Log.e(TAG, "Failed handling calibrate result", it) }
    }

    private suspend fun maybeNotifySlump(event: PostureEvent) {
        val alertsEnabled = app.userPreferencesRepository.alertsEnabled.first()
        if (!alertsEnabled) return

        val alertPrefs = app.userPreferencesRepository.alertPreferences.first()
        if (alertPrefs.phoneNotificationEnabled) {
            app.notificationManager.showSlumpAlert(event.durationSeconds)
        }
    }

    private companion object {
        const val TAG = "KeepStraightPhone"
    }
}
