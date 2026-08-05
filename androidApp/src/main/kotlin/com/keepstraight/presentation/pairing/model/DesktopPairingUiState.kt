package com.keepstraight.presentation.pairing.model

import com.keepstraight.bridge.PhonePairError

data class DesktopPairingUiState(
    val phase: DesktopPairingPhase = DesktopPairingPhase.IDLE,
    val error: PhonePairError? = null,
    val errorDetail: String? = null,
)
