package com.keepstraight.presentation.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keepstraight.KeepStraightApp
import com.keepstraight.shared.application.phone.PhoneWatchSettingsUseCase
import com.keepstraight.data.model.DashboardDayStats
import com.keepstraight.presentation.common.PhonePresentationConfig
import com.keepstraight.presentation.dashboard.DashboardConfig
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), false)

    val pairedWatchId: StateFlow<String?> = app.userPreferencesRepository.pairedWatchId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), null)

    val batteryOptimizationDismissed: StateFlow<Boolean> =
        app.userPreferencesRepository.batteryOptimizationDismissed
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), false)

    private val batteryOptimizationNeeded = MutableStateFlow(batteryProbe.isOptimizationRequired())

    val showBatteryBanner: StateFlow<Boolean> = combine(
        batteryOptimizationNeeded,
        batteryOptimizationDismissed,
    ) { needed, dismissed ->
        needed && !dismissed
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), false)

    private val workStatsFromMs =
        System.currentTimeMillis() - DashboardConfig.WORK_STATS_LOOKBACK_DAYS * DashboardConfig.MS_PER_DAY
    val dashboardDays: StateFlow<List<DashboardDayStats>> =
        app.postureHistoryRepository.workStatsFrom(workStatsFromMs)
            .map { stats -> app.postureHistoryRepository.dashboardDays(stats) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), emptyList())

    fun refreshBatteryBanner() {
        batteryOptimizationNeeded.value = batteryProbe.isOptimizationRequired()
    }

    fun dismissBatteryOptimizationBanner() {
        viewModelScope.launch { settingsUseCase.dismissBatteryOptimizationBanner() }
    }
}
