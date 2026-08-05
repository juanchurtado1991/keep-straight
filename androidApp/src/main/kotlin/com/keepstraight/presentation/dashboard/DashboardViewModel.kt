package com.keepstraight.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepstraight.data.PostureHistoryRepository
import com.keepstraight.data.model.DashboardDayStats
import com.keepstraight.presentation.common.PhonePresentationConfig
import com.keepstraight.shared.application.phone.PhoneWatchSettingsUseCase
import com.keepstraight.shared.platform.BatteryOptimizationProbe
import com.keepstraight.shared.repository.DeviceSyncGateway
import com.keepstraight.shared.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val settingsUseCase: PhoneWatchSettingsUseCase,
    deviceSyncGateway: DeviceSyncGateway,
    userPreferencesRepository: PreferencesRepository,
    postureHistoryRepository: PostureHistoryRepository,
    private val batteryProbe: BatteryOptimizationProbe,
) : ViewModel() {

    val isConnected: StateFlow<Boolean> = deviceSyncGateway.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), false)

    val pairedWatchId: StateFlow<String?> = userPreferencesRepository.pairedWatchId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), null)

    val batteryOptimizationDismissed: StateFlow<Boolean> =
        userPreferencesRepository.batteryOptimizationDismissed
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
        postureHistoryRepository.workStatsFrom(workStatsFromMs)
            .map { stats -> postureHistoryRepository.dashboardDays(stats) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), emptyList())

    fun refreshBatteryBanner() {
        batteryOptimizationNeeded.value = batteryProbe.isOptimizationRequired()
    }

    fun dismissBatteryOptimizationBanner() {
        viewModelScope.launch { settingsUseCase.dismissBatteryOptimizationBanner() }
    }
}
