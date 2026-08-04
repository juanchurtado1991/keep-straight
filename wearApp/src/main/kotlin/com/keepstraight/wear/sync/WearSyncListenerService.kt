package com.keepstraight.wear.sync

import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.keepstraight.wear.KeepStraightWearApp

/**
 * Wakes the wear process when the phone sends a Message/DataItem.
 * Handling is delegated to [WearInboundHandler] (also registered while the process is alive).
 */
class WearSyncListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.i(TAG, "Service woke for ${messageEvent.path}")
        val app = application as KeepStraightWearApp
        app.inboundHandler.handlePath(
            path = messageEvent.path,
            payload = messageEvent.data,
            sourceNodeId = messageEvent.sourceNodeId,
        )
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.i(TAG, "Service woke for DataItem change")
        val app = application as KeepStraightWearApp
        app.inboundHandler.onDataChanged(dataEvents)
    }

    private companion object {
        const val TAG = "KeepStraightWear"
    }
}
