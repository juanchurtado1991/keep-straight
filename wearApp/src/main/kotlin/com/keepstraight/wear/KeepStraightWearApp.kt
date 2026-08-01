package com.keepstraight.wear

import android.app.Application
import com.keepstraight.wear.state.MonitoringSession
import com.keepstraight.wear.sync.PendingSyncQueue

class KeepStraightWearApp : Application() {

    lateinit var monitoringSession: MonitoringSession
        private set

    lateinit var pendingSyncQueue: PendingSyncQueue
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        pendingSyncQueue = PendingSyncQueue(this)
        monitoringSession = MonitoringSession(this)
    }

    companion object {
        lateinit var instance: KeepStraightWearApp
            private set
    }
}
