package com.keepstraight.wear.presentation.sync

import com.ghost.serialization.Ghost
import com.keepstraight.shared.sync.SyncPaths
import com.keepstraight.wear.KeepStraightWearApp
import com.keepstraight.wear.service.PostureMonitoringService
import com.keepstraight.wear.sync.WearMessageSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns pending-event flush and inbound message deduplication for phone→watch sync.
 */
class WearSyncCoordinator(
    private val app: KeepStraightWearApp,
    private val scope: CoroutineScope,
) {
    private val flushMutex = Mutex()
    private val recentDeliveryKeys = ArrayDeque<String>()
    private val deliveryLock = Any()

    fun acceptDelivery(path: String, payload: ByteArray, sourceNodeId: String): Boolean {
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

    fun handleSyncRequest(sourceNodeId: String) {
        scope.launch { flushPendingEvents(sourceNodeId) }
    }

    fun handleSyncAck() {
        PostureMonitoringService.cancelRetryCycle(app)
        app.monitoringSession.setPhoneRetryActive(false)
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

    private companion object {
        const val MAX_RECENT_DELIVERIES = 32
    }
}
