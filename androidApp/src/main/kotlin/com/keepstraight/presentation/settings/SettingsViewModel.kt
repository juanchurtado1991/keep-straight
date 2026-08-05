package com.keepstraight.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepstraight.presentation.common.PhonePresentationConfig
import com.keepstraight.shared.application.phone.PairingUseCase
import com.keepstraight.shared.application.phone.PhoneWatchSettingsUseCase
import com.keepstraight.shared.model.AlertPreferences
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.repository.DeviceSyncGateway
import com.keepstraight.shared.repository.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsUseCase: PhoneWatchSettingsUseCase,
    private val pairingUseCase: PairingUseCase,
    userPreferencesRepository: PreferencesRepository,
    deviceSyncGateway: DeviceSyncGateway,
) : ViewModel() {

    val pairedWatchId: StateFlow<String?> = userPreferencesRepository.pairedWatchId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), null)

    val isConnected: StateFlow<Boolean> = deviceSyncGateway.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), false)

    val monitoringEnabled: StateFlow<Boolean> = userPreferencesRepository.monitoringEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), true)

    val alertsEnabled: StateFlow<Boolean> = userPreferencesRepository.alertsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), true)

    val alertPreferences: StateFlow<AlertPreferences> = userPreferencesRepository.alertPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), AlertPreferences())

    val sensitivity: StateFlow<SensitivityLevel> = userPreferencesRepository.sensitivity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), SensitivityLevel.NORMAL)

    val slumpDurationThresholdMs: StateFlow<Long> =
        userPreferencesRepository.slumpDurationThresholdMs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), SettingsDefaults.SLUMP_DURATION_MS)

    val repeatAlertIntervalMs: StateFlow<Long> =
        userPreferencesRepository.repeatAlertIntervalMs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), SettingsDefaults.REPEAT_ALERT_MS)

    fun setMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsUseCase.setMonitoringEnabled(enabled) }
    }

    fun setAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsUseCase.setAlertsEnabled(enabled) }
    }

    fun setAlertPreferences(preferences: AlertPreferences) {
        viewModelScope.launch { settingsUseCase.updateAlertPreferences(preferences) }
    }

    fun setSensitivity(level: SensitivityLevel) {
        viewModelScope.launch { settingsUseCase.setSensitivity(level) }
    }

    fun setSlumpTiming(slumpDurationThresholdMs: Long, repeatAlertIntervalMs: Long) {
        viewModelScope.launch {
            settingsUseCase.setSlumpTiming(slumpDurationThresholdMs, repeatAlertIntervalMs)
        }
    }

    fun unpairWatch() {
        viewModelScope.launch { pairingUseCase.unpairDevice() }
    }

    fun dismissBatteryOptimizationBanner() {
        viewModelScope.launch { settingsUseCase.dismissBatteryOptimizationBanner() }
    }
}
