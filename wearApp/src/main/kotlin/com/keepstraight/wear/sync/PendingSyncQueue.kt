package com.keepstraight.wear.sync

import android.content.Context
import com.ghost.serialization.Ghost
import com.keepstraight.shared.model.PostureEvent
import com.keepstraight.shared.model.PostureEventBatch
import java.io.File
import java.nio.ByteBuffer

class PendingSyncQueue(context: Context) {

    private val queueFile = File(context.filesDir, QUEUE_FILENAME)
    private val lock = Any()

    fun enqueue(event: PostureEvent) {
        val payload = Ghost.encodeToBytes(event)
        synchronized(lock) {
            queueFile.appendBytes(lengthPrefix(payload) + payload)
        }
    }

    fun reenqueue(batch: PostureEventBatch) {
        if (batch.events.isEmpty()) return
        synchronized(lock) {
            for (event in batch.events) {
                val payload = Ghost.encodeToBytes(event)
                queueFile.appendBytes(lengthPrefix(payload) + payload)
            }
        }
    }

    /** Atomically reads all queued events and clears the file before returning. */
    fun takeBatch(): PostureEventBatch? {
        synchronized(lock) {
            val events = readEventsLocked()
            if (events.isEmpty()) return null
            queueFile.writeBytes(ByteArray(0))
            return PostureEventBatch(events = events)
        }
    }

    fun takeBatchBytes(): ByteArray? {
        val batch = takeBatch() ?: return null
        return Ghost.encodeToBytes(batch)
    }

    fun clear() {
        synchronized(lock) {
            if (queueFile.exists()) {
                queueFile.writeBytes(ByteArray(0))
            }
        }
    }

    fun hasPending(): Boolean {
        synchronized(lock) {
            return queueFile.exists() && queueFile.length() > 0L
        }
    }

    private fun readEventsLocked(): List<PostureEvent> {
        if (!queueFile.exists() || queueFile.length() == 0L) return emptyList()

        val bytes = queueFile.readBytes()
        val events = mutableListOf<PostureEvent>()
        var offset = 0
        while (offset + LENGTH_BYTES <= bytes.size) {
            val length = ByteBuffer.wrap(bytes, offset, LENGTH_BYTES).int
            offset += LENGTH_BYTES
            if (length <= 0 || offset + length > bytes.size) break
            val eventBytes = bytes.copyOfRange(offset, offset + length)
            offset += length
            events.add(Ghost.deserialize(eventBytes))
        }
        return events
    }

    private fun lengthPrefix(payload: ByteArray): ByteArray =
        ByteBuffer.allocate(LENGTH_BYTES).putInt(payload.size).array()

    private companion object {
        const val QUEUE_FILENAME = "pending_sync.bin"
        const val LENGTH_BYTES = 4
    }
}
