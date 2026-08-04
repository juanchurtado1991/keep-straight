package com.keepstraight.shared.bridge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DesktopPairingQrTest {
    @Test
    fun roundTrip() {
        val offer = DesktopPairOffer(
            hosts = listOf("192.168.1.20", "10.0.0.5"),
            port = 8743,
            nonce = "abc123def456",
        )
        val encoded = DesktopPairingQr.encode(offer)
        val parsed = DesktopPairingQr.parse(encoded)
        assertNotNull(parsed)
        assertEquals(offer.hosts, parsed.hosts)
        assertEquals(offer.port, parsed.port)
        assertEquals(offer.nonce, parsed.nonce)
    }

    @Test
    fun rejectsGarbage() {
        assertNull(DesktopPairingQr.parse("https://example.com"))
        assertNull(DesktopPairingQr.parse("keepstraight://desktop-pair?v=1"))
    }
}
