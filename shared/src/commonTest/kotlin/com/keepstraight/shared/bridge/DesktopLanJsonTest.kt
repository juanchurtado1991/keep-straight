package com.keepstraight.shared.bridge

import com.keepstraight.shared.model.SensitivityLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopLanJsonTest {
    @Test
    fun ping_roundTrip() {
        val ping = DesktopLanPingResponse(ok = true, protocolVersion = DesktopLanProtocol.VERSION)
        val parsed = DesktopLanJson.parsePing(DesktopLanJson.pingToJson(ping))
        assertEquals(ping, parsed)
    }

    @Test
    fun ping_parsesLegacyWireJson() {
        val parsed = DesktopLanJson.parsePing("""{"ok":true,"protocolVersion":1}""")
        assertEquals(DesktopLanPingResponse(ok = true, protocolVersion = 1), parsed)
    }

    @Test
    fun ack_roundTrip() {
        val parsed = DesktopLanJson.parseAck(DesktopLanJson.ackToJson())
        assertEquals(DesktopLanAckResponse(), parsed)
    }

    @Test
    fun ack_parsesLegacyWireJson() {
        val parsed = DesktopLanJson.parseAck("""{"ok":true}""")
        assertEquals(DesktopLanAckResponse(), parsed)
    }

    @Test
    fun pairRequest_roundTrip() {
        val req = DesktopPairRequest(code = "123456", protocolVersion = DesktopLanProtocol.VERSION)
        val parsed = DesktopLanJson.parsePairRequest(DesktopLanJson.pairRequestToJson(req))
        assertEquals(req, parsed)
    }

    @Test
    fun pairRequest_parsesLegacyWireJson() {
        val parsed = DesktopLanJson.parsePairRequest("""{"code":"123456","protocolVersion":1}""")
        assertEquals(DesktopPairRequest(code = "123456", protocolVersion = 1), parsed)
    }

    @Test
    fun pairResponse_success_roundTrip() {
        val res = DesktopPairResponse(ok = true, token = "tok-abc")
        val parsed = DesktopLanJson.parsePairResponse(DesktopLanJson.pairResponseToJson(res))
        assertEquals(res, parsed)
    }

    @Test
    fun pairResponse_success_parsesLegacySpacedOk() {
        val parsed = DesktopLanJson.parsePairResponse("""{"ok": true,"token":"abc"}""")
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
        val parsed = DesktopLanJson.parsePairResponse(DesktopLanJson.pairResponseToJson(res))
        assertEquals(res, parsed)
    }

    @Test
    fun pairResponse_rejectsInvalidPayload() {
        assertNull(DesktopLanJson.parsePairResponse("not json"))
        assertNull(DesktopLanJson.parsePairResponse("""{"token":"only"}"""))
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
        val parsed = DesktopLanJson.parseSettings(DesktopLanJson.settingsToJson(settings))
        assertEquals(settings, parsed)
    }

    @Test
    fun settings_parsesLegacyWireJson() {
        val json =
            """{"sensitivity":"NORMAL","slumpDurationThresholdMs":30000,"repeatAlertIntervalMs":5000,"alertsEnabled":true,"protocolVersion":1}"""
        val parsed = DesktopLanJson.parseSettings(json)
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
        val parsed = DesktopLanJson.parseEvent(DesktopLanJson.eventToJson(event))
        assertEquals(event, parsed)
    }

    @Test
    fun slumpEvent_parsesLegacyFullWireJson() {
        val json =
            """{"type":"WORK_SAMPLE","slumpScore":0.0,"presence":"SITTING","timestampMs":99,"protocolVersion":1,"seatedDeltaSec":10,"goodPostureDeltaSec":7}"""
        val parsed = DesktopLanJson.parseEvent(json)
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
        val parsed = DesktopLanJson.parsePhoneHello(DesktopLanJson.phoneHelloToJson(hello))
        assertEquals(hello, parsed)
    }

    @Test
    fun phoneHelloResponse_roundTrip() {
        val ok = PhoneHelloResponse(ok = true)
        val fail = PhoneHelloResponse(ok = false, errorCode = BridgeProtocolError.PAIRING_FAILED)
        assertEquals(ok, DesktopLanJson.parsePhoneHelloResponse(DesktopLanJson.phoneHelloResponseToJson(ok)))
        assertEquals(fail, DesktopLanJson.parsePhoneHelloResponse(DesktopLanJson.phoneHelloResponseToJson(fail)))
    }

    @Test
    fun phoneHelloResponse_parsesLegacySpacedOk() {
        val parsed = DesktopLanJson.parsePhoneHelloResponse("""{"ok": true}""")
        assertNotNull(parsed)
        assertTrue(parsed.ok)
        assertNull(parsed.errorCode)
    }

    @Test
    fun pairResponseJson_containsRequiredFields() {
        val json = DesktopLanJson.pairResponseToJson(
            DesktopPairResponse(ok = true, token = "secret"),
        )
        assertTrue(json.contains("\"ok\""))
        assertTrue(json.contains("secret"))
    }

    @Test
    fun pairFailureJson_omitsNullErrorCodeOnSuccess() {
        val json = DesktopLanJson.pairResponseToJson(DesktopPairResponse(ok = true, token = "x"))
        assertFalse(json.contains("errorCode"))
    }
}
