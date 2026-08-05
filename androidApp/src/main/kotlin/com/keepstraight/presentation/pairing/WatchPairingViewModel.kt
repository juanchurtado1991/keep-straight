package com.keepstraight.presentation.pairing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keepstraight.KeepStraightApp
import com.keepstraight.presentation.common.PhonePresentationConfig
import com.keepstraight.presentation.pairing.WatchPairingConfig
import com.keepstraight.shared.application.phone.PairingUseCase
import com.keepstraight.shared.presentation.DiscoverError
import com.keepstraight.shared.presentation.DiscoverUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class WatchPairingViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val app = application as KeepStraightApp
    private val pairingUseCase = PairingUseCase(
        app.userPreferencesRepository,
        app.syncManager,
    )

    val pairedWatchId: StateFlow<String?> = app.userPreferencesRepository.pairedWatchId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), null)

    private val _discoverState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Idle)
    val discoverState: StateFlow<DiscoverUiState> = _discoverState.asStateFlow()

    private var discoverJob: Job? = null

    fun refreshWatchNodes() {
        discoverJob?.cancel()
        discoverJob = viewModelScope.launch {
            _discoverState.value = DiscoverUiState.Loading
            try {
                val nodes = withTimeout(WatchPairingConfig.DISCOVER_TIMEOUT_MS) {
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
            pairingUseCase.pairDevice(nodeId)
            app.syncManager.refreshConnectionStatus()
        }
    }

    fun unpairWatch() {
        viewModelScope.launch {
            pairingUseCase.unpairDevice()
        }
    }
}
