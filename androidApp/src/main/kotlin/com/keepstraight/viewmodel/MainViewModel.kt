package com.keepstraight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.keepstraight.KeepStraightApp
import com.keepstraight.data.local.PostureEventEntity
import com.keepstraight.shared.model.AlertPreferences
import com.keepstraight.shared.model.CalibrationCaptureResult
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.presentation.CalibrationUiState
import com.keepstraight.shared.repository.PairedDevice
import com.keepstraight.shared.usecase.phone.CalibrationController
import com.keepstraight.shared.usecase.phone.CompleteOnboardingUseCase
import com.keepstraight.shared.usecase.phone.PairingUseCase
import com.keepstraight.shared.usecase.phone.PhoneWatchSettingsUseCase
import com.keepstraight.util.AndroidBatteryOptimizationProbe
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as KeepStraightApp

    private val settingsUseCase = PhoneWatchSettingsUseCase(
        app.userPreferencesRepository,
        app.syncManager,
    )
    private val pairingUseCase = PairingUseCase(
        app.userPreferencesRepository,
        app.syncManager,
    )
    private val completeOnboardingUseCase = CompleteOnboardingUseCase(
        app.userPreferencesRepository,
        app.syncManager,
    )
    private val calibrationController = CalibrationController(
        app.userPreferencesRepository,
        app.syncManager,
    )
    private val batteryProbe = AndroidBatteryOptimizationProbe(application)

    val onboardingComplete: StateFlow<Boolean> = app.userPreferencesRepository.onboardingComplete
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isConnected: StateFlow<Boolean> = app.syncManager.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val sensitivity: StateFlow<SensitivityLevel> = app.userPreferencesRepository.sensitivity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SensitivityLevel.NORMAL)

    val monitoringEnabled: StateFlow<Boolean> = app.userPreferencesRepository.monitoringEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val alertsEnabled: StateFlow<Boolean> = app.userPreferencesRepository.alertsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val alertPreferences: StateFlow<AlertPreferences> = app.userPreferencesRepository.alertPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertPreferences())

    val pairedWatchId: StateFlow<String?> = app.userPreferencesRepository.pairedWatchId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val batteryOptimizationDismissed: StateFlow<Boolean> =
        app.userPreferencesRepository.batteryOptimizationDismissed
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _batteryOptimizationNeeded = MutableStateFlow(batteryProbe.isOptimizationRequired())

    val showBatteryBanner: StateFlow<Boolean> = combine(
        _batteryOptimizationNeeded,
        batteryOptimizationDismissed,
    ) { needed, dismissed ->
        needed && !dismissed
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val eventsPaged: Flow<PagingData<PostureEventEntity>> =
        app.postureHistoryRepository.eventsPaged().cachedIn(viewModelScope)

    private val _availableNodes = MutableStateFlow<List<PairedDevice>>(emptyList())
    val availableNodes: StateFlow<List<PairedDevice>> = _availableNodes.asStateFlow()

    val calibrationState: StateFlow<CalibrationUiState> = calibrationController.state

    val calibrationResult = calibrationController.calibrationResult

    private var calibrationJob: Job? = null

    fun refreshBatteryBanner() {
        _batteryOptimizationNeeded.value = batteryProbe.isOptimizationRequired()
    }

    fun refreshWatchNodes() {
        viewModelScope.launch {
            _availableNodes.value = pairingUseCase.discoverDevices()
        }
    }

    fun pairWatch(nodeId: String) {
        viewModelScope.launch {
            pairingUseCase.pairDevice(nodeId)
        }
    }

    fun unpairWatch() {
        viewModelScope.launch {
            pairingUseCase.unpairDevice()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            completeOnboardingUseCase()
        }
    }

    fun setSensitivity(level: SensitivityLevel) {
        viewModelScope.launch {
            settingsUseCase.setSensitivity(level)
        }
    }

    fun setMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCase.setMonitoringEnabled(enabled)
        }
    }

    fun setAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCase.setAlertsEnabled(enabled)
        }
    }

    fun setAlertPreferences(preferences: AlertPreferences) {
        viewModelScope.launch {
            settingsUseCase.updateAlertPreferences(preferences)
        }
    }

    fun dismissBatteryOptimizationBanner() {
        viewModelScope.launch {
            settingsUseCase.dismissBatteryOptimizationBanner()
        }
    }

    fun reconnectWatch() {
        viewModelScope.launch {
            settingsUseCase.reconnectWatch()
        }
    }

    fun requestSync() {
        viewModelScope.launch {
            settingsUseCase.requestSync()
        }
    }

    fun startCalibrationCountdown() {
        calibrationJob?.cancel()
        calibrationJob = viewModelScope.launch {
            calibrationController.start(isConnected.value)
        }
    }

    fun onCalibrationResult(result: CalibrationCaptureResult) {
        viewModelScope.launch {
            calibrationController.onResult(result)
        }
    }

    fun resetCalibrationState() {
        calibrationController.reset()
    }

    fun isBatteryOptimizationEnabled(): Boolean = batteryProbe.isOptimizationRequired()
}
