package com.keepstraight.di

import android.app.Application
import com.keepstraight.bridge.AndroidDesktopPairingGateway
import com.keepstraight.bridge.PhoneLanIngestServer
import com.keepstraight.data.PostureHistoryRepository
import com.keepstraight.data.UserPreferencesRepository
import com.keepstraight.data.local.PostureDatabase
import com.keepstraight.notifications.PostureNotificationManager
import com.keepstraight.presentation.calibration.CalibrationViewModel
import com.keepstraight.presentation.connection.ConnectionViewModel
import com.keepstraight.presentation.dashboard.DashboardViewModel
import com.keepstraight.presentation.onboarding.OnboardingViewModel
import com.keepstraight.presentation.pairing.DesktopPairingViewModel
import com.keepstraight.presentation.pairing.WatchPairingViewModel
import com.keepstraight.presentation.settings.SettingsViewModel
import com.keepstraight.presentation.shell.AppShellViewModel
import com.keepstraight.shared.di.sharedDomainModule
import com.keepstraight.shared.platform.BatteryOptimizationProbe
import com.keepstraight.shared.repository.DeviceSyncGateway
import com.keepstraight.shared.repository.DesktopPairingGateway
import com.keepstraight.shared.repository.PreferencesRepository
import com.keepstraight.sync.PhoneWearSyncManager
import com.keepstraight.util.AndroidBatteryOptimizationProbe
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

val androidDataModule = module {
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

    single<PreferencesRepository> { get<UserPreferencesRepository>() }
    single<DeviceSyncGateway> { get<PhoneWearSyncManager>() }
    single { AndroidDesktopPairingGateway(get()) }
    single<DesktopPairingGateway> { get<AndroidDesktopPairingGateway>() }
    single { AndroidBatteryOptimizationProbe(androidContext()) }
    single<BatteryOptimizationProbe> { get<AndroidBatteryOptimizationProbe>() }
}

val androidPresentationModule = module {
    viewModel { AppShellViewModel(get(), get()) }
    viewModel { DashboardViewModel(get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get()) }
    viewModel { OnboardingViewModel(get(), get(), get(), get()) }
    viewModel { WatchPairingViewModel(get(), get(), get()) }
    viewModel { DesktopPairingViewModel(get()) }
    viewModel { ConnectionViewModel(get(), get(), get()) }
    viewModel { CalibrationViewModel(get(), get(), get()) }
}

val androidAppModule = module {
    includes(androidDataModule, androidPresentationModule)
}

fun startKeepStraightKoin(application: Application) {
    startKoin {
        androidContext(application)
        modules(sharedDomainModule, androidAppModule)
    }
}
