package com.keepstraight.wear

import android.app.Application
import android.util.Log
import com.keepstraight.wear.di.startWearKoin
import com.keepstraight.wear.presentation.monitoring.WearMonitoringStore
import com.keepstraight.wear.presentation.sync.WearSyncCoordinator
import com.keepstraight.wear.state.MonitoringSession
import com.keepstraight.wear.sync.PendingSyncQueue
import com.keepstraight.wear.sync.WearInboundHandler
import org.koin.android.ext.android.getKoin

class KeepStraightWearApp : Application() {

    val monitoringSession: MonitoringSession
        get() = getKoin().get()

    val monitoringStore: WearMonitoringStore
        get() = getKoin().get()

    val syncCoordinator: WearSyncCoordinator
        get() = getKoin().get()

    val pendingSyncQueue: PendingSyncQueue
        get() = getKoin().get()

    val inboundHandler: WearInboundHandler
        get() = getKoin().get()

    override fun onCreate() {
        super.onCreate()
        instance = this
        startWearKoin(this)
        inboundHandler.start()
        Log.i(TAG, "Wear app process started")
    }

    companion object {
        private const val TAG = "KeepStraightWear"

        lateinit var instance: KeepStraightWearApp
            private set
    }
}
