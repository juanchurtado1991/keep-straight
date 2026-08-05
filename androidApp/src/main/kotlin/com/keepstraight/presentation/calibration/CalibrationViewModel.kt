package com.keepstraight.presentation.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepstraight.shared.application.phone.CalibrationController
import com.keepstraight.shared.application.phone.RefreshWatchConnectionUseCase
import com.keepstraight.shared.presentation.CalibrationUiState
import com.keepstraight.shared.presentation.common.FeatureStore
import com.keepstraight.shared.presentation.phone.CalibrationEffect
import com.keepstraight.shared.presentation.phone.CalibrationEvent
import com.keepstraight.shared.repository.DeviceSyncGateway
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.keepstraight.presentation.common.PhonePresentationConfig

class CalibrationViewModel(
    private val calibrationController: CalibrationController,
    private val refreshConnection: RefreshWatchConnectionUseCase,
    deviceSyncGateway: DeviceSyncGateway,
) : ViewModel(),
    FeatureStore<CalibrationUiState, CalibrationEvent, CalibrationEffect> {

    override val state: StateFlow<CalibrationUiState> = calibrationController.state

    private val _effects = MutableSharedFlow<CalibrationEffect>(extraBufferCapacity = 1)
    override val effects: SharedFlow<CalibrationEffect> = _effects.asSharedFlow()

    val isConnected: StateFlow<Boolean> = deviceSyncGateway.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), false)

    private var calibrationJob: Job? = null

    override fun onEvent(event: CalibrationEvent) {
        when (event) {
            CalibrationEvent.Start -> launchStart()
            CalibrationEvent.ContinueSlouch -> launchContinueSlouch()
            CalibrationEvent.Reset -> launchReset()
            CalibrationEvent.SuccessAcknowledged -> launchNavigateBack()
        }
    }

    private fun launchStart() {
        calibrationJob?.cancel()
        calibrationJob = viewModelScope.launch {
            val connected = refreshConnection()
            calibrationController.start(connected)
        }
    }

    private fun launchContinueSlouch() {
        calibrationJob?.cancel()
        calibrationJob = viewModelScope.launch {
            val connected = refreshConnection()
            calibrationController.continueWithSlouch(connected)
        }
    }

    private fun launchReset() {
        calibrationJob?.cancel()
        calibrationController.reset()
    }

    private fun launchNavigateBack() {
        viewModelScope.launch {
            _effects.emit(CalibrationEffect.NavigateBack)
        }
    }
}
