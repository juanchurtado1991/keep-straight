package com.keepstraight

import android.app.Application
import com.keepstraight.bridge.PhoneLanIngestServer
import com.keepstraight.data.PostureHistoryRepository
import com.keepstraight.data.UserPreferencesRepository
import com.keepstraight.data.local.PostureDatabase
import com.keepstraight.notifications.PostureNotificationManager
import com.keepstraight.sync.PhoneWearSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KeepStraightApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: PostureDatabase
        private set

    lateinit var postureHistoryRepository: PostureHistoryRepository
        private set

    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    lateinit var syncManager: PhoneWearSyncManager
        private set

    lateinit var notificationManager: PostureNotificationManager
        private set

    lateinit var lanIngestServer: PhoneLanIngestServer
        private set

    override fun onCreate() {
        super.onCreate()
        database = PostureDatabase.create(this)
        postureHistoryRepository = PostureHistoryRepository(database)
        userPreferencesRepository = UserPreferencesRepository(this)
        syncManager = PhoneWearSyncManager(this, userPreferencesRepository)
        notificationManager = PostureNotificationManager(this)
        lanIngestServer = PhoneLanIngestServer(
            context = this,
            historyRepository = postureHistoryRepository,
            syncManager = syncManager,
            preferencesRepository = userPreferencesRepository,
        )
        // PhoneLanBridgeService (FGS) starts the Ktor server from MainActivity / boot / pair.
        appScope.launch {
            // Old posture_events lack seated/good hours — inject stable mock into new table.
            postureHistoryRepository.ensureWorkHourStatsSeeded()
        }
    }
}
