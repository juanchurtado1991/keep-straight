package com.keepstraight.desktop.di

import com.keepstraight.desktop.DesktopSessionController
import com.keepstraight.shared.di.sharedDomainModule
import org.koin.dsl.module
import java.util.prefs.Preferences

fun desktopAppModule(prefs: Preferences) = module {
    includes(sharedDomainModule)
    single { prefs }
    single { DesktopSessionController(get()) }
}
