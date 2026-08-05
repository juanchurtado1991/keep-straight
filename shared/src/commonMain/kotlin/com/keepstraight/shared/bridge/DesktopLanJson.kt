package com.keepstraight.shared.bridge

import com.ghost.serialization.Ghost

/**
 * JSON encode/decode for the desktop↔phone LAN HTTP bridge via [Ghost].
 * Call sites keep these helpers so wire usage stays in one place.
 */
object DesktopLanJson {
    fun pingToJson(response: DesktopLanPingResponse): String = Ghost.encodeToString(response)

    fun parsePing(json: String): DesktopLanPingResponse? =
        runCatching { Ghost.deserialize<DesktopLanPingResponse>(json.trim()) }.getOrNull()

    fun ackToJson(response: DesktopLanAckResponse = DesktopLanAckResponse()): String =
        Ghost.encodeToString(response)

    fun parseAck(json: String): DesktopLanAckResponse? =
        runCatching { Ghost.deserialize<DesktopLanAckResponse>(json.trim()) }.getOrNull()

    fun eventToJson(event: DesktopSlumpEvent): String = Ghost.encodeToString(event)

    fun parseEvent(json: String): DesktopSlumpEvent? =
        runCatching { Ghost.deserialize<DesktopSlumpEvent>(json.trim()) }.getOrNull()

    fun pairRequestToJson(req: DesktopPairRequest): String = Ghost.encodeToString(req)

    fun parsePairRequest(json: String): DesktopPairRequest? =
        runCatching { Ghost.deserialize<DesktopPairRequest>(json.trim()) }.getOrNull()

    fun pairResponseToJson(res: DesktopPairResponse): String = Ghost.encodeToString(res)

    fun parsePairResponse(json: String): DesktopPairResponse? =
        runCatching { Ghost.deserialize<DesktopPairResponse>(json.trim()) }.getOrNull()

    fun settingsToJson(settings: DesktopPhoneSettings): String = Ghost.encodeToString(settings)

    fun parseSettings(json: String): DesktopPhoneSettings? =
        runCatching { Ghost.deserialize<DesktopPhoneSettings>(json.trim()) }.getOrNull()

    fun phoneHelloToJson(req: PhoneHelloRequest): String = Ghost.encodeToString(req)

    fun parsePhoneHello(json: String): PhoneHelloRequest? =
        runCatching { Ghost.deserialize<PhoneHelloRequest>(json.trim()) }.getOrNull()

    fun phoneHelloResponseToJson(res: PhoneHelloResponse): String = Ghost.encodeToString(res)

    fun parsePhoneHelloResponse(json: String): PhoneHelloResponse? =
        runCatching { Ghost.deserialize<PhoneHelloResponse>(json.trim()) }.getOrNull()
}
