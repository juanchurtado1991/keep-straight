package com.keepstraight.shared.bridge

import com.ghost.serialization.annotations.GhostSerialization

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

@GhostSerialization
data class PhoneHelloRequest(
    val nonce: String,
    val code: String,
    val phoneHosts: List<String>,
    val phonePort: Int = DesktopLanProtocol.DEFAULT_PORT,
    val protocolVersion: Int = DesktopLanProtocol.VERSION,
)

@GhostSerialization
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

@GhostSerialization
enum class DesktopSlumpEventType {
    SLUMP_INITIAL,
    SLUMP_REPEAT,
    SESSION_STARTED,
    SESSION_STOPPED,
    PRESENCE_CHANGED,
    /** Accumulated seated desk time + good-posture time since last sample. */
    WORK_SAMPLE,
}

@GhostSerialization
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

@GhostSerialization
data class DesktopPairRequest(
    val code: String,
    val protocolVersion: Int = DesktopLanProtocol.VERSION,
)

@GhostSerialization
data class DesktopPairResponse(
    val ok: Boolean,
    val token: String = "",
    val errorCode: BridgeProtocolError? = null,
)

@GhostSerialization
data class DesktopPhoneSettings(
    val sensitivity: String = "NORMAL",
    val slumpDurationThresholdMs: Long = 30_000L,
    val repeatAlertIntervalMs: Long = 5_000L,
    val alertsEnabled: Boolean = true,
    val protocolVersion: Int = DesktopLanProtocol.VERSION,
)
