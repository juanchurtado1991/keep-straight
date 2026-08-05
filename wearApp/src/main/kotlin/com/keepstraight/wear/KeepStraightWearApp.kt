package com.keepstraight.wear

import android.app.Application
import android.util.Log
import com.keepstraight.wear.presentation.monitoring.WearMonitoringStore
import com.keepstraight.wear.presentation.sync.WearSyncCoordinator
import com.keepstraight.wear.state.MonitoringSession
import com.keepstraight.wear.sync.PendingSyncQueue
import com.keepstraight.wear.sync.WearInboundHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class KeepStraightWearApp : Application() {

    lateinit var monitoringSession: MonitoringSession
        private set

    lateinit var monitoringStore: WearMonitoringStore
        private set

    lateinit var syncCoordinator: WearSyncCoordinator
        private set

    lateinit var pendingSyncQueue: PendingSyncQueue
        private set

    lateinit var inboundHandler: WearInboundHandler
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        pendingSyncQueue = PendingSyncQueue(this)
        monitoringSession = MonitoringSession(this)
        monitoringStore = WearMonitoringStore(monitoringSession)
        syncCoordinator = WearSyncCoordinator(
            app = this,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        )
        inboundHandler = WearInboundHandler(this).also { it.start() }
        Log.i(TAG, "Wear app process started")
    }

    companion object {
        private const val TAG = "KeepStraightWear"

        lateinit var instance: KeepStraightWearApp
            private set
    }
}
