package com.keepstraight.presentation.pairing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keepstraight.KeepStraightApp
import com.keepstraight.bridge.AndroidDesktopPairingGateway
import com.keepstraight.bridge.PhonePairException
import com.keepstraight.bridge.PhonePairError
import com.keepstraight.bridge.phonePairErrorFromProtocol
import com.keepstraight.presentation.pairing.model.DesktopPairingPhase
import com.keepstraight.presentation.pairing.model.DesktopPairingUiState
import com.keepstraight.shared.application.phone.PairDesktopFailure
import com.keepstraight.shared.application.phone.PairDesktopUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

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
            _state.update { it.copy(phase = DesktopPairingPhase.PAIRING, error = null, errorDetail = null) }
            pairDesktop.pairFromQrPayload(raw).fold(
                onSuccess = {
                    _state.update { it.copy(phase = DesktopPairingPhase.SUCCESS) }
                },
                onFailure = { err ->
                    handled.set(false)
                    val pairError = when (err) {
                        is PairDesktopFailure -> phonePairErrorFromProtocol(err.error)
                        is PhonePairException -> err.error
                        else -> phonePairErrorFromProtocol(null)
                    }
                    val phase = when (pairError) {
                        PhonePairError.INVALID_QR -> DesktopPairingPhase.INVALID_QR
                        else -> DesktopPairingPhase.FAILED
                    }
                    _state.update {
                        it.copy(
                            phase = phase,
                            error = pairError,
                            errorDetail = null,
                        )
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
