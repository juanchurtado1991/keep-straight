package com.keepstraight.wear.sync

import android.content.Context
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class WearMessageSender(private val context: Context) {

    suspend fun getPhoneNodeId(): String? {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        return nodes.firstOrNull { it.isNearby }?.id ?: nodes.firstOrNull()?.id
    }

    suspend fun sendToPhone(path: String, data: ByteArray): Boolean {
        val nodeId = getPhoneNodeId() ?: return false
        return sendToNode(nodeId, path, data)
    }

    suspend fun sendToNode(nodeId: String, path: String, data: ByteArray): Boolean {
        return try {
            Wearable.getMessageClient(context)
                .sendMessage(nodeId, path, data)
                .await()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getConnectedNodes(): List<Node> =
        Wearable.getNodeClient(context).connectedNodes.await()
}
