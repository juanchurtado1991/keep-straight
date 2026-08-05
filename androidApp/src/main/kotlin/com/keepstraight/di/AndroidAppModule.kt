package com.keepstraight.di

import android.app.Application
import com.keepstraight.bridge.PhoneLanIngestServer
import com.keepstraight.data.PostureHistoryRepository
import com.keepstraight.data.UserPreferencesRepository
import com.keepstraight.data.local.PostureDatabase
import com.keepstraight.notifications.PostureNotificationManager
import com.keepstraight.sync.PhoneWearSyncManager
import com.keepstraight.shared.di.sharedDomainModule
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidAppModule = module {
    single { PostureDatabase.create(androidContext()) }
    single { PostureHistoryRepository(get()) }
    single { UserPreferencesRepository(androidContext()) }
    single { PhoneWearSyncManager(androidContext(), get()) }
    single { PostureNotificationManager(androidContext()) }
    single {
        PhoneLanIngestServer(
            context = androidContext(),
            historyRepository = get(),
            syncManager = get(),
            preferencesRepository = get(),
        )
    }
}

fun startKeepStraightKoin(application: Application) {
    org.koin.core.context.startKoin {
        androidContext(application)
        modules(sharedDomainModule, androidAppModule)
    }
}
