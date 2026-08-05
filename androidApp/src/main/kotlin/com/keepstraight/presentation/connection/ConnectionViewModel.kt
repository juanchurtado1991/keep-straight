package com.keepstraight.presentation.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepstraight.presentation.common.PhonePresentationConfig
import com.keepstraight.shared.application.phone.ReconnectWatchUseCase
import com.keepstraight.shared.presentation.ReconnectError
import com.keepstraight.shared.presentation.ReconnectUiState
import com.keepstraight.shared.repository.DeviceSyncFailureReason
import com.keepstraight.shared.repository.DeviceSyncGateway
import com.keepstraight.shared.repository.PreferencesRepository
import com.keepstraight.shared.repository.deviceSyncFailureReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

import java.util.concurrent.TimeoutException

class ConnectionViewModel(
    private val reconnectWatchUseCase: ReconnectWatchUseCase,
    userPreferencesRepository: PreferencesRepository,
    private val deviceSyncGateway: DeviceSyncGateway,
) : ViewModel() {

    val pairedWatchId: StateFlow<String?> = userPreferencesRepository.pairedWatchId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), null)

    val isConnected: StateFlow<Boolean> = deviceSyncGateway.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), false)

    private val _reconnectState = MutableStateFlow<ReconnectUiState>(ReconnectUiState.Idle)
    val reconnectState: StateFlow<ReconnectUiState> = _reconnectState.asStateFlow()

    private var reconnectJob: Job? = null

    fun reconnectWatch() {
        if (_reconnectState.value is ReconnectUiState.InProgress) return
        reconnectJob?.cancel()
        reconnectJob = viewModelScope.launch {
            _reconnectState.value = ReconnectUiState.InProgress
            val result = try {
                withTimeout(ConnectionConfig.RECONNECT_TIMEOUT_MS) {
                    reconnectWatchUseCase()
                }
            } catch (error: TimeoutCancellationException) {
                Result.failure(error)
            } catch (error: Exception) {
                Result.failure(error)
            }

            val connected = deviceSyncGateway.refreshConnectionStatus()
            if (result.isSuccess && connected) {
                _reconnectState.value = ReconnectUiState.Success
            } else if (result.isSuccess) {
                _reconnectState.value = ReconnectUiState.Failed(ReconnectError.WATCH_UNREACHABLE)
            } else {
                _reconnectState.value = ReconnectUiState.Failed(mapReconnectError(result.exceptionOrNull()))
            }
        }
    }

    fun onScreenOpened() {
        if (_reconnectState.value is ReconnectUiState.Success) {
            _reconnectState.value = ReconnectUiState.Idle
        }
    }

    fun clearReconnectError() {
        if (_reconnectState.value !is ReconnectUiState.InProgress) {
            _reconnectState.value = ReconnectUiState.Idle
        }
    }

    private fun mapReconnectError(error: Throwable?): ReconnectError = when (error?.deviceSyncFailureReason()) {
        DeviceSyncFailureReason.NO_PAIRED_WATCH -> ReconnectError.NO_PAIRED_WATCH
        DeviceSyncFailureReason.WATCH_UNREACHABLE -> ReconnectError.WATCH_UNREACHABLE
        null -> when (error) {
            is TimeoutCancellationException, is TimeoutException -> ReconnectError.SEND_TIMEOUT
            else -> ReconnectError.SEND_FAILED
        }
    }
}
