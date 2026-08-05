package com.keepstraight.shared.bridge

/**
 * Phase 2 LAN bridge: desktop → phone (same Wi‑Fi), then phone → watch via Wear.
 * Protocol version must match on both ends.
 */
object DesktopLanProtocol {
    const val VERSION = 1
    const val DEFAULT_PORT = 8742
    /** Ephemeral desktop listener so the phone can register after scanning the QR. */
    const val PAIR_ASSIST_PORT = 8743
    const val PATH_PING = "/v1/ping"
    const val PATH_EVENT = "/v1/slump-event"
    const val PATH_PAIR = "/v1/pair"
    /** Phone is source of truth for sensitivity + timers (phase 2). */
    const val PATH_SETTINGS = "/v1/settings"
    /** Phone → desktop after scanning QR. */
    const val PATH_PHONE_HELLO = "/v1/phone-hello"
    const val HEADER_TOKEN = "X-KeepStraight-Token"
}

data class DesktopPairOffer(
    val hosts: List<String>,
    val port: Int = DesktopLanProtocol.PAIR_ASSIST_PORT,
    val nonce: String,
)

data class PhoneHelloRequest(
    val nonce: String,
    val code: String,
    val phoneHosts: List<String>,
    val phonePort: Int = DesktopLanProtocol.DEFAULT_PORT,
    val protocolVersion: Int = DesktopLanProtocol.VERSION,
)

data class PhoneHelloResponse(
    val ok: Boolean,
    val errorCode: BridgeProtocolError? = null,
)

/**
 * QR payload shown on desktop, scanned by phone:
 * keepstraight://desktop-pair?v=1&hosts=a,b&port=8743&nonce=...
 */
object DesktopPairingQr {
    private const val SCHEME = "keepstraight"
    private const val HOST = "desktop-pair"

    fun encode(offer: DesktopPairOffer): String {
        val hosts = offer.hosts.joinToString(",")
        return "$SCHEME://$HOST?v=${DesktopLanProtocol.VERSION}" +
            "&hosts=$hosts" +
            "&port=${offer.port}" +
            "&nonce=${offer.nonce}"
    }

    fun parse(raw: String): DesktopPairOffer? {
        val text = raw.trim()
        if (!text.startsWith("$SCHEME://$HOST")) return null
        val query = text.substringAfter('?', missingDelimiterValue = "").ifBlank { return null }
        val params = query.split('&').mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx <= 0) null
            else part.substring(0, idx) to part.substring(idx + 1)
        }.toMap()
        val hosts = params["hosts"].orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val nonce = params["nonce"].orEmpty()
        val port = params["port"]?.toIntOrNull() ?: DesktopLanProtocol.PAIR_ASSIST_PORT
        if (hosts.isEmpty() || nonce.isBlank()) return null
        return DesktopPairOffer(hosts = hosts, port = port, nonce = nonce)
    }
}

enum class DesktopSlumpEventType {
    SLUMP_INITIAL,
    SLUMP_REPEAT,
    SESSION_STARTED,
    SESSION_STOPPED,
    PRESENCE_CHANGED,
    /** Accumulated seated desk time + good-posture time since last sample. */
    WORK_SAMPLE,
}

data class DesktopSlumpEvent(
    val type: DesktopSlumpEventType,
    val slumpScore: Float = 0f,
    val presence: String = "SITTING",
    val timestampMs: Long,
    val protocolVersion: Int = DesktopLanProtocol.VERSION,
    /** Seconds seated at desk in this sample (WORK_SAMPLE). */
    val seatedDeltaSec: Int = 0,
    /** Seconds seated with good posture in this sample (WORK_SAMPLE). */
    val goodPostureDeltaSec: Int = 0,
)

data class DesktopPairRequest(
    val code: String,
    val protocolVersion: Int = DesktopLanProtocol.VERSION,
)

data class DesktopPairResponse(
    val ok: Boolean,
    val token: String = "",
    val errorCode: BridgeProtocolError? = null,
)

data class DesktopPhoneSettings(
    val sensitivity: String = "NORMAL",
    val slumpDurationThresholdMs: Long = 30_000L,
    val repeatAlertIntervalMs: Long = 5_000L,
    val alertsEnabled: Boolean = true,
    val protocolVersion: Int = DesktopLanProtocol.VERSION,
)

/** Minimal JSON helpers (no kotlinx.serialization required). */
object DesktopLanJson {
    fun eventToJson(event: DesktopSlumpEvent): String =
        """{"type":"${event.type.name}","slumpScore":${event.slumpScore},"presence":"${event.presence}","timestampMs":${event.timestampMs},"protocolVersion":${event.protocolVersion},"seatedDeltaSec":${event.seatedDeltaSec},"goodPostureDeltaSec":${event.goodPostureDeltaSec}}"""

    fun parseEvent(json: String): DesktopSlumpEvent? {
        val type = stringField(json, "type") ?: return null
        val eventType = runCatching { DesktopSlumpEventType.valueOf(type) }.getOrNull() ?: return null
        return DesktopSlumpEvent(
            type = eventType,
            slumpScore = floatField(json, "slumpScore") ?: 0f,
            presence = stringField(json, "presence") ?: "SITTING",
            timestampMs = longField(json, "timestampMs") ?: 0L,
            protocolVersion = intField(json, "protocolVersion") ?: 0,
            seatedDeltaSec = intField(json, "seatedDeltaSec") ?: 0,
            goodPostureDeltaSec = intField(json, "goodPostureDeltaSec") ?: 0,
        )
    }

    fun pairRequestToJson(req: DesktopPairRequest): String =
        """{"code":"${req.code}","protocolVersion":${req.protocolVersion}}"""

    fun parsePairRequest(json: String): DesktopPairRequest? {
        val code = stringField(json, "code") ?: return null
        return DesktopPairRequest(
            code = code,
            protocolVersion = intField(json, "protocolVersion") ?: 0,
        )
    }

    fun pairResponseToJson(res: DesktopPairResponse): String {
        val codeJson = res.errorCode?.let { ""","errorCode":"${it.name}"""" }.orEmpty()
        return """{"ok":${res.ok},"token":"${res.token}"$codeJson}"""
    }

    fun parsePairResponse(json: String): DesktopPairResponse? {
        val ok = DesktopLanJsonWire.isOkTrue(json)
        return DesktopPairResponse(
            ok = ok,
            token = stringField(json, "token").orEmpty(),
            errorCode = parseErrorCode(json),
        )
    }

    fun settingsToJson(settings: DesktopPhoneSettings): String =
        """{"sensitivity":"${settings.sensitivity}","slumpDurationThresholdMs":${settings.slumpDurationThresholdMs},"repeatAlertIntervalMs":${settings.repeatAlertIntervalMs},"alertsEnabled":${settings.alertsEnabled},"protocolVersion":${settings.protocolVersion}}"""

    fun parseSettings(json: String): DesktopPhoneSettings? {
        val sensitivity = stringField(json, "sensitivity") ?: return null
        return DesktopPhoneSettings(
            sensitivity = sensitivity,
            slumpDurationThresholdMs = longField(json, "slumpDurationThresholdMs") ?: 30_000L,
            repeatAlertIntervalMs = longField(json, "repeatAlertIntervalMs") ?: 5_000L,
            alertsEnabled = booleanField(json, "alertsEnabled") ?: true,
            protocolVersion = intField(json, "protocolVersion") ?: 0,
        )
    }

    fun phoneHelloToJson(req: PhoneHelloRequest): String {
        val hosts = req.phoneHosts.joinToString(",") { "\"$it\"" }
        return """{"nonce":"${req.nonce}","code":"${req.code}","phoneHosts":[$hosts],"phonePort":${req.phonePort},"protocolVersion":${req.protocolVersion}}"""
    }

    fun parsePhoneHello(json: String): PhoneHelloRequest? {
        val nonce = stringField(json, "nonce") ?: return null
        val code = stringField(json, "code") ?: return null
        val hosts = stringArrayField(json, "phoneHosts")
        if (hosts.isEmpty()) return null
        return PhoneHelloRequest(
            nonce = nonce,
            code = code,
            phoneHosts = hosts,
            phonePort = intField(json, "phonePort") ?: DesktopLanProtocol.DEFAULT_PORT,
            protocolVersion = intField(json, "protocolVersion") ?: 0,
        )
    }

    fun phoneHelloResponseToJson(res: PhoneHelloResponse): String {
        val codeJson = res.errorCode?.let { ""","errorCode":"${it.name}"""" }.orEmpty()
        return """{"ok":${res.ok}$codeJson}"""
    }

    fun parsePhoneHelloResponse(json: String): PhoneHelloResponse =
        PhoneHelloResponse(
            ok = DesktopLanJsonWire.isOkTrue(json),
            errorCode = parseErrorCode(json),
        )

    private fun parseErrorCode(json: String): BridgeProtocolError? =
        stringField(json, "errorCode")?.let { raw ->
            runCatching { BridgeProtocolError.valueOf(raw) }.getOrNull()
        }

    private fun stringArrayField(json: String, key: String): List<String> {
        val regex = Regex("\"$key\"\\s*:\\s*\\[(.*?)]")
        val inner = regex.find(json)?.groupValues?.get(1) ?: return emptyList()
        return Regex("\"([^\"]*)\"").findAll(inner).map { it.groupValues[1] }.toList()
    }

    private fun stringField(json: String, key: String): String? {
        val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        return regex.find(json)?.groupValues?.get(1)
    }

    private fun floatField(json: String, key: String): Float? {
        val regex = Regex("\"$key\"\\s*:\\s*(-?[0-9.]+)")
        return regex.find(json)?.groupValues?.get(1)?.toFloatOrNull()
    }

    private fun longField(json: String, key: String): Long? {
        val regex = Regex("\"$key\"\\s*:\\s*(-?[0-9]+)")
        return regex.find(json)?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun intField(json: String, key: String): Int? =
        longField(json, key)?.toInt()

    private fun booleanField(json: String, key: String): Boolean? {
        val regex = Regex("\"$key\"\\s*:\\s*(true|false)")
        return regex.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }
}
