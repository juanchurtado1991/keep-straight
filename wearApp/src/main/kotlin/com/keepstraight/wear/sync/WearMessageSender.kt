package com.keepstraight.wear.sync

import android.content.Context
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class WearMessageSender(private val context: Context) {

    suspend fun getPhoneNodeId(): String? {
        val nodes = awaitWear { Wearable.getNodeClient(context).connectedNodes.await() }
            ?: return null
        return nodes.firstOrNull { it.isNearby }?.id ?: nodes.firstOrNull()?.id
    }

    suspend fun sendToPhone(path: String, data: ByteArray): Boolean {
        val nodeId = getPhoneNodeId() ?: return false
        return sendToNode(nodeId, path, data)
    }

    suspend fun sendToNode(nodeId: String, path: String, data: ByteArray): Boolean {
        return try {
            withTimeout(WEAR_TIMEOUT_MS) {
                Wearable.getMessageClient(context)
                    .sendMessage(nodeId, path, data)
                    .await()
            }
            true
        } catch (_: TimeoutCancellationException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getConnectedNodes(): List<Node> =
        awaitWear { Wearable.getNodeClient(context).connectedNodes.await() } ?: emptyList()

    private suspend fun <T> awaitWear(block: suspend () -> T): T? = try {
        withTimeout(WEAR_TIMEOUT_MS) { block() }
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val WEAR_TIMEOUT_MS = 8_000L
    }
}
