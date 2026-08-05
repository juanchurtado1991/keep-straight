package com.keepstraight.shared.repository

import com.keepstraight.shared.bridge.DesktopPairOffer

interface DesktopPairingGateway {
    suspend fun pairByScanningDesktopQr(offer: DesktopPairOffer): Result<Unit>

    fun close()
}
