package com.keepstraight.presentation.connection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keepstraight.KeepStraightApp
import com.keepstraight.presentation.common.PhonePresentationConfig
import com.keepstraight.shared.application.phone.ReconnectWatchUseCase
import com.keepstraight.shared.presentation.ReconnectError
import com.keepstraight.shared.presentation.ReconnectUiState
import com.keepstraight.sync.PhoneWearSyncManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeoutException

class ConnectionViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val app = application as KeepStraightApp
    private val reconnectWatchUseCase = ReconnectWatchUseCase(app.syncManager)

    val pairedWatchId: StateFlow<String?> = app.userPreferencesRepository.pairedWatchId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), null)

    val isConnected: StateFlow<Boolean> = app.syncManager.isConnected
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
}
