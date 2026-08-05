package com.keepstraight.presentation.calibration

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keepstraight.KeepStraightApp
import com.keepstraight.shared.application.phone.CalibrationController
import com.keepstraight.shared.application.phone.RefreshWatchConnectionUseCase
import com.keepstraight.shared.presentation.CalibrationUiState
import com.keepstraight.shared.presentation.common.FeatureStore
import com.keepstraight.shared.presentation.phone.CalibrationEffect
import com.keepstraight.shared.presentation.phone.CalibrationEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class CalibrationViewModel(
    application: Application,
) : AndroidViewModel(application),
    FeatureStore<CalibrationUiState, CalibrationEvent, CalibrationEffect> {

    private val app = application as KeepStraightApp
    private val calibrationController = CalibrationController(
        app.userPreferencesRepository,
        app.syncManager,
    )
    private val refreshConnection = RefreshWatchConnectionUseCase(app.syncManager)

    override val state: StateFlow<CalibrationUiState> = calibrationController.state

    private val _effects = MutableSharedFlow<CalibrationEffect>(extraBufferCapacity = 1)
    override val effects: SharedFlow<CalibrationEffect> = _effects.asSharedFlow()

    val isConnected = app.syncManager.isConnected

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
