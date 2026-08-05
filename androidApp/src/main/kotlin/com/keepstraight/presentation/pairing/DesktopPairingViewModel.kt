package com.keepstraight.presentation.pairing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keepstraight.KeepStraightApp
import com.keepstraight.bridge.AndroidDesktopPairingGateway
import com.keepstraight.shared.application.phone.PairDesktopUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

enum class DesktopPairingPhase {
    IDLE,
    PAIRING,
    SUCCESS,
    FAILED,
    INVALID_QR,
}

data class DesktopPairingUiState(
    val phase: DesktopPairingPhase = DesktopPairingPhase.IDLE,
    val errorMessage: String? = null,
)

class DesktopPairingViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val app = application as KeepStraightApp
    private val pairDesktop = PairDesktopUseCase(
        AndroidDesktopPairingGateway(app.lanIngestServer),
    )

    private val _state = MutableStateFlow(DesktopPairingUiState())
    val state: StateFlow<DesktopPairingUiState> = _state.asStateFlow()

    private val handled = AtomicBoolean(false)

    fun onQrPayload(raw: String) {
        if (!handled.compareAndSet(false, true)) return
        viewModelScope.launch {
            _state.update { it.copy(phase = DesktopPairingPhase.PAIRING, errorMessage = null) }
            pairDesktop.pairFromQrPayload(raw).fold(
                onSuccess = {
                    _state.update { it.copy(phase = DesktopPairingPhase.SUCCESS) }
                },
                onFailure = { err ->
                    handled.set(false)
                    val phase = if (err is IllegalArgumentException) {
                        DesktopPairingPhase.INVALID_QR
                    } else {
                        DesktopPairingPhase.FAILED
                    }
                    _state.update {
                        it.copy(phase = phase, errorMessage = err.message)
                    }
                },
            )
        }
    }

    override fun onCleared() {
        pairDesktop.close()
        super.onCleared()
    }
}
