package com.keepstraight.bridge

import com.keepstraight.shared.bridge.DesktopPairOffer
import com.keepstraight.shared.repository.DesktopPairingGateway

class AndroidDesktopPairingGateway(
    lanIngestServer: PhoneLanIngestServer,
) : DesktopPairingGateway {
    private val client = PhoneDesktopPairClient(lanIngestServer)

    override suspend fun pairByScanningDesktopQr(offer: DesktopPairOffer): Result<Unit> =
        client.pairByScanningDesktopQr(offer)

    override fun close() {
        client.close()
    }
}
