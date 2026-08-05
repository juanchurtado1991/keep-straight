package com.keepstraight.wear.di

import android.app.Application
import com.keepstraight.wear.KeepStraightWearApp
import com.keepstraight.wear.presentation.monitoring.WearMonitoringStore
import com.keepstraight.wear.presentation.sync.WearSyncCoordinator
import com.keepstraight.wear.state.MonitoringSession
import com.keepstraight.wear.sync.PendingSyncQueue
import com.keepstraight.wear.sync.WearInboundHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val wearIoScope = named("wearIoScope")

val wearAppModule = module {
    single { PendingSyncQueue(androidContext()) }
    single { MonitoringSession(androidContext()) }
    single { WearMonitoringStore(get()) }
    single(wearIoScope) { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    single {
        WearSyncCoordinator(
            app = androidContext() as KeepStraightWearApp,
            scope = get(wearIoScope),
        )
    }
    single { WearInboundHandler(androidContext() as KeepStraightWearApp) }
}

fun startWearKoin(application: Application) {
    startKoin {
        androidContext(application)
        modules(wearAppModule)
    }
}
