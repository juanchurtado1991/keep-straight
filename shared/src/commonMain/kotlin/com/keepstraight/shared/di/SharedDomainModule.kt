package com.keepstraight.shared.di

import com.keepstraight.shared.application.phone.CompleteOnboardingUseCase
import com.keepstraight.shared.application.phone.PairDesktopUseCase
import com.keepstraight.shared.application.phone.PairingUseCase
import com.keepstraight.shared.application.phone.PhoneWatchSettingsUseCase
import com.keepstraight.shared.application.phone.ReconnectWatchUseCase
import com.keepstraight.shared.application.phone.RefreshWatchConnectionUseCase
import org.koin.dsl.module

/** Shared domain registrations — platform modules supply gateways and stores. */
val sharedDomainModule = module {
    factory { PairingUseCase(get(), get()) }
    factory { PhoneWatchSettingsUseCase(get(), get()) }
    factory { PairDesktopUseCase(get()) }
    factory { ReconnectWatchUseCase(get()) }
    factory { RefreshWatchConnectionUseCase(get()) }
    factory { CompleteOnboardingUseCase(get(), get()) }
}
