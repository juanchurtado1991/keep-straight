package com.keepstraight.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.keepstraight.KeepStraightApp
import com.keepstraight.data.local.PostureEventEntity
import com.keepstraight.shared.model.AlertPreferences
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.presentation.CalibrationUiState
import com.keepstraight.shared.presentation.DiscoverError
import com.keepstraight.shared.presentation.DiscoverUiState
import com.keepstraight.shared.presentation.ReconnectError
import com.keepstraight.shared.presentation.ReconnectUiState
import com.keepstraight.shared.usecase.phone.CalibrationController
import com.keepstraight.shared.usecase.phone.CompleteOnboardingUseCase
import com.keepstraight.shared.usecase.phone.PairingUseCase
import com.keepstraight.shared.usecase.phone.PhoneWatchSettingsUseCase
import com.keepstraight.sync.PhoneWearSyncManager
import com.keepstraight.util.AndroidBatteryOptimizationProbe
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeoutException

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

    val slumpDurationThresholdMs: StateFlow<Long> =
        app.userPreferencesRepository.slumpDurationThresholdMs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 30_000L)

    val repeatAlertIntervalMs: StateFlow<Long> =
        app.userPreferencesRepository.repeatAlertIntervalMs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5_000L)

    val monitoringEnabled: StateFlow<Boolean> = app.userPreferencesRepository.monitoringEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val alertsEnabled: StateFlow<Boolean> = app.userPreferencesRepository.alertsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val alertPreferences: StateFlow<AlertPreferences> = app.userPreferencesRepository.alertPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertPreferences())

    val pairedWatchId: StateFlow<String?> = app.userPreferencesRepository.pairedWatchId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val hasCalibration: StateFlow<Boolean> = combine(
        app.userPreferencesRepository.calibrationPitch,
        app.userPreferencesRepository.calibrationRoll,
    ) { pitch, roll -> pitch != null && roll != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

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

    private val workStatsFromMs = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000
    val dashboardDays: StateFlow<List<com.keepstraight.data.DashboardDayStats>> =
        app.postureHistoryRepository.workStatsFrom(workStatsFromMs)
            .map { stats -> app.postureHistoryRepository.dashboardDays(stats) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _discoverState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Idle)
    val discoverState: StateFlow<DiscoverUiState> = _discoverState.asStateFlow()

    private val _reconnectState = MutableStateFlow<ReconnectUiState>(ReconnectUiState.Idle)
    val reconnectState: StateFlow<ReconnectUiState> = _reconnectState.asStateFlow()

    val calibrationState: StateFlow<CalibrationUiState> = calibrationController.state

    private var calibrationJob: Job? = null
    private var reconnectJob: Job? = null
    private var discoverJob: Job? = null

    fun refreshBatteryBanner() {
        _batteryOptimizationNeeded.value = batteryProbe.isOptimizationRequired()
    }

    fun refreshWatchNodes() {
        discoverJob?.cancel()
        discoverJob = viewModelScope.launch {
            _discoverState.value = DiscoverUiState.Loading
            try {
                val nodes = withTimeout(DISCOVER_TIMEOUT_MS) {
                    pairingUseCase.discoverDevices()
                }
                _discoverState.value = DiscoverUiState.Ready(nodes)
            } catch (_: TimeoutCancellationException) {
                _discoverState.value = DiscoverUiState.Failed(DiscoverError.TIMEOUT)
            } catch (_: Exception) {
                _discoverState.value = DiscoverUiState.Failed(DiscoverError.FAILED)
            }
        }
    }

    fun pairWatch(nodeId: String) {
        viewModelScope.launch {
            // Replacing the paired id clears the previous watch association.
            pairingUseCase.pairDevice(nodeId)
            app.syncManager.refreshConnectionStatus()
            _reconnectState.value = ReconnectUiState.Idle
        }
    }

    fun unpairWatch() {
        viewModelScope.launch {
            pairingUseCase.unpairDevice()
            _reconnectState.value = ReconnectUiState.Idle
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

    fun setSlumpTiming(slumpDurationThresholdMs: Long, repeatAlertIntervalMs: Long) {
        viewModelScope.launch {
            settingsUseCase.setSlumpTiming(slumpDurationThresholdMs, repeatAlertIntervalMs)
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
        if (_reconnectState.value is ReconnectUiState.InProgress) return
        reconnectJob?.cancel()
        reconnectJob = viewModelScope.launch {
            _reconnectState.value = ReconnectUiState.InProgress
            val result = try {
                withTimeout(RECONNECT_TIMEOUT_MS) {
                    app.syncManager.reconnect()
                }
            } catch (error: TimeoutCancellationException) {
                Result.failure(error)
            } catch (error: Exception) {
                Result.failure(error)
            }

            if (result.isSuccess) {
                app.syncManager.refreshConnectionStatus()
                _reconnectState.value = ReconnectUiState.Success
            } else {
                _reconnectState.value = ReconnectUiState.Failed(mapReconnectError(result.exceptionOrNull()))
            }
        }
    }

    fun clearReconnectError() {
        if (_reconnectState.value !is ReconnectUiState.InProgress) {
            _reconnectState.value = ReconnectUiState.Idle
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
            val connected = app.syncManager.refreshConnectionStatus()
            calibrationController.start(connected)
        }
    }

    fun continueSlouchCalibration() {
        calibrationJob?.cancel()
        calibrationJob = viewModelScope.launch {
            val connected = app.syncManager.refreshConnectionStatus()
            calibrationController.continueWithSlouch(connected)
        }
    }

    fun resetCalibrationState() {
        calibrationJob?.cancel()
        calibrationController.reset()
    }

    fun isBatteryOptimizationEnabled(): Boolean = batteryProbe.isOptimizationRequired()

    private fun mapReconnectError(error: Throwable?): ReconnectError {
        val message = error?.message.orEmpty()
        return when {
            message.contains(PhoneWearSyncManager.ERROR_NO_PAIRED) -> ReconnectError.NO_PAIRED_WATCH
            message.contains(PhoneWearSyncManager.ERROR_UNREACHABLE) -> ReconnectError.WATCH_UNREACHABLE
            error is TimeoutCancellationException || error is TimeoutException ->
                ReconnectError.SEND_TIMEOUT
            else -> ReconnectError.SEND_FAILED
        }
    }

    private companion object {
        const val DISCOVER_TIMEOUT_MS = 8_000L
        const val RECONNECT_TIMEOUT_MS = 25_000L
    }
}
