package com.keepstraight.shared.application.phone

import com.keepstraight.shared.bridge.DesktopPairingQr
import com.keepstraight.shared.repository.DesktopPairingGateway

class PairDesktopUseCase(
    private val gateway: DesktopPairingGateway,
) {
    suspend fun pairFromQrPayload(raw: String): Result<Unit> {
        val offer = DesktopPairingQr.parse(raw)
            ?: return Result.failure(IllegalArgumentException("Invalid desktop pairing QR"))
        return gateway.pairByScanningDesktopQr(offer)
    }

    fun close() {
        gateway.close()
    }
}
