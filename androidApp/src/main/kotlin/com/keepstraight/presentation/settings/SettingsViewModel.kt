package com.keepstraight.presentation.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keepstraight.KeepStraightApp
import com.keepstraight.shared.application.phone.PairingUseCase
import com.keepstraight.shared.application.phone.PhoneWatchSettingsUseCase
import com.keepstraight.shared.model.AlertPreferences
import com.keepstraight.shared.model.SensitivityLevel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val app = application as KeepStraightApp
    private val settingsUseCase = PhoneWatchSettingsUseCase(
        app.userPreferencesRepository,
        app.syncManager,
    )
    private val pairingUseCase = PairingUseCase(
        app.userPreferencesRepository,
        app.syncManager,
    )

    val pairedWatchId: StateFlow<String?> = app.userPreferencesRepository.pairedWatchId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isConnected: StateFlow<Boolean> = app.syncManager.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val monitoringEnabled: StateFlow<Boolean> = app.userPreferencesRepository.monitoringEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val alertsEnabled: StateFlow<Boolean> = app.userPreferencesRepository.alertsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val alertPreferences: StateFlow<AlertPreferences> = app.userPreferencesRepository.alertPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertPreferences())

    val sensitivity: StateFlow<SensitivityLevel> = app.userPreferencesRepository.sensitivity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SensitivityLevel.NORMAL)

    val slumpDurationThresholdMs: StateFlow<Long> =
        app.userPreferencesRepository.slumpDurationThresholdMs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 30_000L)

    val repeatAlertIntervalMs: StateFlow<Long> =
        app.userPreferencesRepository.repeatAlertIntervalMs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5_000L)

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

    fun requestSync() {
        viewModelScope.launch { settingsUseCase.requestSync() }
    }

    fun dismissBatteryOptimizationBanner() {
        viewModelScope.launch { settingsUseCase.dismissBatteryOptimizationBanner() }
    }
}
