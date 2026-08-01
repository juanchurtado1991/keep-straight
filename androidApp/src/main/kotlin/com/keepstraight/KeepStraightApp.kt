package com.keepstraight

import android.app.Application
import com.keepstraight.data.PostureHistoryRepository
import com.keepstraight.data.UserPreferencesRepository
import com.keepstraight.data.local.PostureDatabase
import com.keepstraight.notifications.PostureNotificationManager
import com.keepstraight.sync.PhoneWearSyncManager

class KeepStraightApp : Application() {

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

    override fun onCreate() {
        super.onCreate()
        database = PostureDatabase.create(this)
        postureHistoryRepository = PostureHistoryRepository(database)
        userPreferencesRepository = UserPreferencesRepository(this)
        syncManager = PhoneWearSyncManager(this, userPreferencesRepository)
        notificationManager = PostureNotificationManager(this)
    }
}
