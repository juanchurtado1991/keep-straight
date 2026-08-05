package com.keepstraight.shared.bridge

import com.ghost.serialization.Ghost
import com.keepstraight.shared.model.SensitivityLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopLanWireTest {
    @Test
    fun ping_roundTrip() {
        val ping = DesktopLanPingResponse(ok = true, protocolVersion = DesktopLanProtocol.VERSION)
        assertEquals(ping, roundTrip(ping))
    }

    @Test
    fun ping_parsesLegacyWireJson() {
        val parsed = parseWire<DesktopLanPingResponse>("""{"ok":true,"protocolVersion":1}""")
        assertEquals(DesktopLanPingResponse(ok = true, protocolVersion = 1), parsed)
    }

    @Test
    fun ack_roundTrip() {
        assertEquals(DesktopLanAckResponse(), roundTrip(DesktopLanAckResponse()))
    }

    @Test
    fun ack_parsesLegacyWireJson() {
        val parsed = parseWire<DesktopLanAckResponse>("""{"ok":true}""")
        assertEquals(DesktopLanAckResponse(), parsed)
    }

    @Test
    fun pairRequest_roundTrip() {
        val req = DesktopPairRequest(code = "123456", protocolVersion = DesktopLanProtocol.VERSION)
        assertEquals(req, roundTrip(req))
    }

    @Test
    fun pairRequest_parsesLegacyWireJson() {
        val parsed = parseWire<DesktopPairRequest>("""{"code":"123456","protocolVersion":1}""")
        assertEquals(DesktopPairRequest(code = "123456", protocolVersion = 1), parsed)
    }

    @Test
    fun pairResponse_success_roundTrip() {
        val res = DesktopPairResponse(ok = true, token = "tok-abc")
        assertEquals(res, roundTrip(res))
    }

    @Test
    fun pairResponse_success_parsesLegacySpacedOk() {
        val parsed = parseWire<DesktopPairResponse>("""{"ok": true,"token":"abc"}""")
        assertNotNull(parsed)
        assertTrue(parsed.ok)
        assertEquals("abc", parsed.token)
    }

    @Test
    fun pairResponse_failure_roundTrip() {
        val res = DesktopPairResponse(
            ok = false,
            errorCode = BridgeProtocolError.INVALID_CODE,
        )
        assertEquals(res, roundTrip(res))
    }

    @Test
    fun pairResponse_rejectsInvalidPayload() {
        assertNull(parseWire<DesktopPairResponse>("not json"))
        assertNull(parseWire<DesktopPairResponse>("""{"token":"only"}"""))
    }

    @Test
    fun settings_roundTrip() {
        val settings = DesktopPhoneSettings(
            sensitivity = SensitivityLevel.STRICT,
            slumpDurationThresholdMs = 45_000L,
            repeatAlertIntervalMs = 12_000L,
            alertsEnabled = false,
            protocolVersion = DesktopLanProtocol.VERSION,
        )
        assertEquals(settings, roundTrip(settings))
    }

    @Test
    fun settings_parsesLegacyWireJson() {
        val json =
            """{"sensitivity":"NORMAL","slumpDurationThresholdMs":30000,"repeatAlertIntervalMs":5000,"alertsEnabled":true,"protocolVersion":1}"""
        val parsed = parseWire<DesktopPhoneSettings>(json)
        assertNotNull(parsed)
        assertEquals(SensitivityLevel.NORMAL, parsed.sensitivity)
        assertEquals(30_000L, parsed.slumpDurationThresholdMs)
    }

    @Test
    fun slumpEvent_roundTrip() {
        val event = DesktopSlumpEvent(
            type = DesktopSlumpEventType.SLUMP_INITIAL,
            slumpScore = 0.42f,
            presence = "SITTING",
            timestampMs = 1_700_000_000_000L,
            protocolVersion = DesktopLanProtocol.VERSION,
        )
        assertEquals(event, roundTrip(event))
    }

    @Test
    fun slumpEvent_parsesLegacyFullWireJson() {
        val json =
            """{"type":"WORK_SAMPLE","slumpScore":0.0,"presence":"SITTING","timestampMs":99,"protocolVersion":1,"seatedDeltaSec":10,"goodPostureDeltaSec":7}"""
        val parsed = parseWire<DesktopSlumpEvent>(json)
        assertEquals(
            DesktopSlumpEvent(
                type = DesktopSlumpEventType.WORK_SAMPLE,
                timestampMs = 99L,
                seatedDeltaSec = 10,
                goodPostureDeltaSec = 7,
                protocolVersion = 1,
            ),
            parsed,
        )
    }

    @Test
    fun phoneHello_roundTrip() {
        val hello = PhoneHelloRequest(
            nonce = "nonce-1",
            code = "654321",
            phoneHosts = listOf("192.168.1.20", "10.0.0.5"),
            phonePort = 8742,
            protocolVersion = DesktopLanProtocol.VERSION,
        )
        assertEquals(hello, roundTrip(hello))
    }

    @Test
    fun phoneHelloResponse_roundTrip() {
        val ok = PhoneHelloResponse(ok = true)
        val fail = PhoneHelloResponse(ok = false, errorCode = BridgeProtocolError.PAIRING_FAILED)
        assertEquals(ok, roundTrip(ok))
        assertEquals(fail, roundTrip(fail))
    }

    @Test
    fun phoneHelloResponse_parsesLegacySpacedOk() {
        val parsed = parseWire<PhoneHelloResponse>("""{"ok": true}""")
        assertNotNull(parsed)
        assertTrue(parsed.ok)
        assertNull(parsed.errorCode)
    }

    @Test
    fun pairResponseJson_containsRequiredFields() {
        val json = Ghost.encodeToString(DesktopPairResponse(ok = true, token = "secret"))
        assertTrue(json.contains("\"ok\""))
        assertTrue(json.contains("secret"))
    }

    @Test
    fun pairFailureJson_omitsNullErrorCodeOnSuccess() {
        val json = Ghost.encodeToString(DesktopPairResponse(ok = true, token = "x"))
        assertFalse(json.contains("errorCode"))
    }

    private inline fun <reified T> parseWire(json: String): T? =
        try {
            Ghost.deserialize(json.trim())
        } catch (_: Exception) {
            null
        }

    private inline fun <reified T : Any> roundTrip(value: T): T? =
        parseWire(Ghost.encodeToString(value))
}
