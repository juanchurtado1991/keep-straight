package com.keepstraight.desktop.bridge

import com.keepstraight.desktop.presentation.DesktopMessageKey
import com.keepstraight.desktop.presentation.DesktopPrefsKeys
import com.keepstraight.shared.bridge.DesktopBridgeClient
import com.keepstraight.shared.bridge.DesktopLanJson
import com.keepstraight.shared.bridge.DesktopLanProtocol
import com.keepstraight.shared.bridge.DesktopPairRequest
import com.keepstraight.shared.bridge.DesktopPhoneSettings
import com.keepstraight.shared.bridge.DesktopSlumpEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import java.util.prefs.Preferences

class JvmDesktopBridgeClient(
    private val prefs: Preferences,
) : DesktopBridgeClient {
    private val client = HttpClient(CIO)

    private var host: String? = prefs.get(DesktopPrefsKeys.BRIDGE_HOST, null)?.ifBlank { null }
    private var port: Int = prefs.getInt(DesktopPrefsKeys.BRIDGE_PORT, DesktopLanProtocol.DEFAULT_PORT)
    private var token: String? = prefs.get(DesktopPrefsKeys.BRIDGE_TOKEN, null)?.ifBlank { null }

    override val isConfigured: Boolean
        get() = !host.isNullOrBlank() && !token.isNullOrBlank()

    override suspend fun pair(host: String, port: Int, code: String): Result<String> {
        return runCatching {
            val response = client.post("http://$host:$port${DesktopLanProtocol.PATH_PAIR}") {
                contentType(ContentType.Application.Json)
                setBody(DesktopLanJson.pairRequestToJson(DesktopPairRequest(code = code)))
            }
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                val parsed = DesktopLanJson.parsePairResponse(body)
                throw BridgeClientException(
                    DesktopMessageKey.BRIDGE_CLIENT_PAIR_FAILED,
                    parsed?.message?.ifBlank { null } ?: response.status.value.toString(),
                )
            }
            val parsed = DesktopLanJson.parsePairResponse(body)
                ?: throw BridgeClientException(DesktopMessageKey.BRIDGE_CLIENT_PAIR_FAILED, "invalid_response")
            if (!parsed.ok) {
                throw BridgeClientException(
                    DesktopMessageKey.BRIDGE_CLIENT_PAIR_REJECTED,
                    parsed.message.ifBlank { null },
                )
            }
            if (parsed.token.isBlank()) {
                throw BridgeClientException(DesktopMessageKey.BRIDGE_CLIENT_MISSING_TOKEN)
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
                contentType(ContentType.Application.Json)
                setBody(DesktopLanJson.eventToJson(event))
            }
            if (response.status.value == 401) {
                throw BridgeClientException(DesktopMessageKey.BRIDGE_CLIENT_UNAUTHORIZED)
            }
            if (!response.status.isSuccess()) {
                throw BridgeClientException(
                    DesktopMessageKey.BRIDGE_CLIENT_PAIR_FAILED,
                    response.status.value.toString(),
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
                throw BridgeClientException(DesktopMessageKey.BRIDGE_CLIENT_UNAUTHORIZED)
            }
            if (!response.status.isSuccess()) {
                throw BridgeClientException(
                    DesktopMessageKey.BRIDGE_CLIENT_PAIR_FAILED,
                    response.status.value.toString(),
                )
            }
            DesktopLanJson.parseSettings(response.bodyAsText())
                ?: throw BridgeClientException(DesktopMessageKey.BRIDGE_CLIENT_PAIR_FAILED, "invalid_settings")
        }
    }

    suspend fun ping(): Result<Unit> {
        val h = host ?: return Result.failure(BridgeClientException(DesktopMessageKey.BRIDGE_CLIENT_NOT_PAIRED))
        return runCatching {
            val response = client.get("http://$h:$port${DesktopLanProtocol.PATH_PING}")
            if (!response.status.isSuccess()) {
                throw BridgeClientException(
                    DesktopMessageKey.BRIDGE_CLIENT_PAIR_FAILED,
                    response.status.value.toString(),
                )
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
}
