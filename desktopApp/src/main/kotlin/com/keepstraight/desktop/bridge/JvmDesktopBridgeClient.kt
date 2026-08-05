package com.keepstraight.desktop.bridge

import com.ghost.serialization.ktor.ghost
import com.keepstraight.desktop.presentation.DesktopMessageKey
import com.keepstraight.desktop.presentation.DesktopPrefsKeys
import com.keepstraight.shared.bridge.BridgeProtocolError
import com.keepstraight.shared.bridge.DesktopBridgeClient
import com.keepstraight.shared.bridge.DesktopLanProtocol
import com.keepstraight.shared.bridge.DesktopLanPingResponse
import com.keepstraight.shared.bridge.DesktopPairRequest
import com.keepstraight.shared.bridge.DesktopPairResponse
import com.keepstraight.shared.bridge.DesktopPhoneSettings
import com.keepstraight.shared.bridge.DesktopSlumpEvent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess
import java.util.prefs.Preferences

class JvmDesktopBridgeClient(
    private val prefs: Preferences,
) : DesktopBridgeClient {
    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = LAN_CONNECT_TIMEOUT_MS
            requestTimeoutMillis = LAN_REQUEST_TIMEOUT_MS
            socketTimeoutMillis = LAN_REQUEST_TIMEOUT_MS
        }
        install(ContentNegotiation) {
            ghost()
        }
    }

    private var host: String? = prefs.get(DesktopPrefsKeys.BRIDGE_HOST, null)?.ifBlank { null }
    private var port: Int = prefs.getInt(DesktopPrefsKeys.BRIDGE_PORT, DesktopLanProtocol.DEFAULT_PORT)
    private var token: String? = prefs.get(DesktopPrefsKeys.BRIDGE_TOKEN, null)?.ifBlank { null }

    override val isConfigured: Boolean
        get() = !host.isNullOrBlank() && !token.isNullOrBlank()

    override suspend fun pair(host: String, port: Int, code: String): Result<String> {
        return runCatching {
            val response = client.post("http://$host:$port${DesktopLanProtocol.PATH_PAIR}") {
                setBody(DesktopPairRequest(code = code))
            }
            val parsed: DesktopPairResponse = try {
                response.body()
            } catch (_: Exception) {
                throw BridgeClientException(
                    DesktopMessageKey.BRIDGE_CLIENT_PAIR_FAILED,
                    BridgeProtocolError.INVALID_RESPONSE,
                )
            }
            if (!response.status.isSuccess()) {
                throw BridgeClientException(
                    DesktopMessageKey.BRIDGE_CLIENT_PAIR_FAILED,
                    parsed.errorCode ?: BridgeProtocolError.PAIRING_FAILED,
                )
            }
            if (!parsed.ok) {
                throw BridgeClientException(
                    DesktopMessageKey.BRIDGE_CLIENT_PAIR_REJECTED,
                    parsed.errorCode ?: BridgeProtocolError.PAIRING_FAILED,
                )
            }
            if (parsed.token.isBlank()) {
                throw BridgeClientException(
                    DesktopMessageKey.BRIDGE_CLIENT_MISSING_TOKEN,
                )
            }
            this.host = host
            this.port = port
            this.token = parsed.token
            prefs.put(DesktopPrefsKeys.BRIDGE_HOST, host)
            prefs.putInt(DesktopPrefsKeys.BRIDGE_PORT, port)
            prefs.put(DesktopPrefsKeys.BRIDGE_TOKEN, parsed.token)
            parsed.token
        }
    }

    override suspend fun sendEvent(event: DesktopSlumpEvent): Result<Unit> {
        if (host == null || token == null) {
            return Result.failure(BridgeClientException(DesktopMessageKey.BRIDGE_CLIENT_NOT_PAIRED))
        }
        val h = host!!
        val t = token!!
        return runCatching {
            val response = client.post("http://$h:$port${DesktopLanProtocol.PATH_EVENT}") {
                header(DesktopLanProtocol.HEADER_TOKEN, t)
                setBody(event)
            }
            if (response.status.value == 401) {
                throw BridgeClientException(
                    DesktopMessageKey.BRIDGE_CLIENT_UNAUTHORIZED,
                    BridgeProtocolError.UNAUTHORIZED,
                )
            }
            if (!response.status.isSuccess()) {
                throw BridgeClientException(
                    DesktopMessageKey.BRIDGE_CLIENT_PAIR_FAILED,
                    BridgeProtocolError.PAIRING_FAILED,
                )
            }
        }
    }

    override suspend fun fetchSettings(): Result<DesktopPhoneSettings> {
        if (host == null || token == null) {
            return Result.failure(BridgeClientException(DesktopMessageKey.BRIDGE_CLIENT_NOT_PAIRED))
        }
        val h = host!!
        val t = token!!
        return runCatching {
            val response = client.get("http://$h:$port${DesktopLanProtocol.PATH_SETTINGS}") {
                header(DesktopLanProtocol.HEADER_TOKEN, t)
            }
            if (response.status.value == 401) {
                throw BridgeClientException(
                    DesktopMessageKey.BRIDGE_CLIENT_UNAUTHORIZED,
                    BridgeProtocolError.UNAUTHORIZED,
                )
            }
            if (!response.status.isSuccess()) {
                throw BridgeClientException(
                    DesktopMessageKey.BRIDGE_CLIENT_PAIR_FAILED,
                    BridgeProtocolError.PAIRING_FAILED,
                )
            }
            try {
                response.body<DesktopPhoneSettings>()
            } catch (_: Exception) {
                throw BridgeClientException(
                    DesktopMessageKey.BRIDGE_CLIENT_PAIR_FAILED,
                    BridgeProtocolError.INVALID_SETTINGS,
                )
            }
        }
    }

    suspend fun ping(): Result<Boolean> {
        val h = host ?: return Result.failure(BridgeClientException(DesktopMessageKey.BRIDGE_CLIENT_NOT_PAIRED))
        return runCatching {
            val response = client.get("http://$h:$port${DesktopLanProtocol.PATH_PING}")
            if (!response.status.isSuccess()) {
                throw BridgeClientException(
                    DesktopMessageKey.BRIDGE_CLIENT_PAIR_FAILED,
                    BridgeProtocolError.PAIRING_FAILED,
                )
            }
            response.body<DesktopLanPingResponse>().bridgeLinked
        }
    }

    override suspend fun notifyRemoteUnpair() {
        val h = host ?: return
        val t = token ?: return
        runCatching {
            client.post("http://$h:$port${DesktopLanProtocol.PATH_UNPAIR}") {
                header(DesktopLanProtocol.HEADER_TOKEN, t)
            }
        }
    }

    override fun clear() {
        host = null
        token = null
        prefs.remove(DesktopPrefsKeys.BRIDGE_HOST)
        prefs.remove(DesktopPrefsKeys.BRIDGE_TOKEN)
        prefs.remove(DesktopPrefsKeys.BRIDGE_PORT)
    }

    fun close() {
        client.close()
    }

    private companion object {
        const val LAN_CONNECT_TIMEOUT_MS = 5_000L
        const val LAN_REQUEST_TIMEOUT_MS = 15_000L
    }
}
