package com.keepstraight.presentation.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keepstraight.KeepStraightApp
import com.keepstraight.shared.application.phone.PhoneWatchSettingsUseCase
import com.keepstraight.util.AndroidBatteryOptimizationProbe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val app = application as KeepStraightApp
    private val settingsUseCase = PhoneWatchSettingsUseCase(
        app.userPreferencesRepository,
        app.syncManager,
    )
    private val batteryProbe = AndroidBatteryOptimizationProbe(application)

    val isConnected: StateFlow<Boolean> = app.syncManager.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val pairedWatchId: StateFlow<String?> = app.userPreferencesRepository.pairedWatchId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val batteryOptimizationDismissed: StateFlow<Boolean> =
        app.userPreferencesRepository.batteryOptimizationDismissed
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val batteryOptimizationNeeded = MutableStateFlow(batteryProbe.isOptimizationRequired())

    val showBatteryBanner: StateFlow<Boolean> = combine(
        batteryOptimizationNeeded,
        batteryOptimizationDismissed,
    ) { needed, dismissed ->
        needed && !dismissed
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val workStatsFromMs = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000
    val dashboardDays: StateFlow<List<com.keepstraight.data.DashboardDayStats>> =
        app.postureHistoryRepository.workStatsFrom(workStatsFromMs)
            .map { stats -> app.postureHistoryRepository.dashboardDays(stats) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun refreshBatteryBanner() {
        batteryOptimizationNeeded.value = batteryProbe.isOptimizationRequired()
    }

    fun dismissBatteryOptimizationBanner() {
        viewModelScope.launch { settingsUseCase.dismissBatteryOptimizationBanner() }
    }
}
