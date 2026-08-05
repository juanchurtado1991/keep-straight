package com.keepstraight.shared.application.phone

import com.keepstraight.shared.bridge.BridgeProtocolError
import com.keepstraight.shared.bridge.DesktopPairingQr
import com.keepstraight.shared.repository.DesktopPairingGateway

class PairDesktopFailure(val error: BridgeProtocolError) : Exception(error.name)

class PairDesktopUseCase(
    private val gateway: DesktopPairingGateway,
) {
    suspend fun pairFromQrPayload(raw: String): Result<Unit> {
        val offer = DesktopPairingQr.parse(raw)
            ?: return Result.failure(PairDesktopFailure(BridgeProtocolError.INVALID_QR))
        return gateway.pairByScanningDesktopQr(offer)
    }

    fun close() {
        gateway.close()
    }
}
