package com.keepstraight

import android.app.Application
import com.keepstraight.bridge.PhoneLanIngestServer
import com.keepstraight.data.PostureHistoryRepository
import com.keepstraight.data.UserPreferencesRepository
import com.keepstraight.data.local.PostureDatabase
import com.keepstraight.di.startKeepStraightKoin
import com.keepstraight.notifications.PostureNotificationManager
import com.keepstraight.sync.PhoneWearSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin

class KeepStraightApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: PostureDatabase
        get() = getKoin().get()

    val postureHistoryRepository: PostureHistoryRepository
        get() = getKoin().get()

    val userPreferencesRepository: UserPreferencesRepository
        get() = getKoin().get()

    val syncManager: PhoneWearSyncManager
        get() = getKoin().get()

    val notificationManager: PostureNotificationManager
        get() = getKoin().get()

    val lanIngestServer: PhoneLanIngestServer
        get() = getKoin().get()

    override fun onCreate() {
        super.onCreate()
        startKeepStraightKoin(this)
        appScope.launch {
            postureHistoryRepository.ensureWorkHourStatsSeeded()
        }
    }
}
