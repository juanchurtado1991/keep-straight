package com.keepstraight.desktop.bridge

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

    private var host: String? = prefs.get("bridge_host", null)?.ifBlank { null }
    private var port: Int = prefs.getInt("bridge_port", DesktopLanProtocol.DEFAULT_PORT)
    private var token: String? = prefs.get("bridge_token", null)?.ifBlank { null }

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
                error(parsed?.message?.ifBlank { null } ?: "Pairing failed (${response.status.value})")
            }
            val parsed = DesktopLanJson.parsePairResponse(body)
                ?: error("Invalid pair response")
            if (!parsed.ok) error(parsed.message.ifBlank { "Pairing rejected" })
            if (parsed.token.isBlank()) error("Pairing response missing token")
            this.host = host
            this.port = port
            this.token = parsed.token
            prefs.put("bridge_host", host)
            prefs.putInt("bridge_port", port)
            prefs.put("bridge_token", parsed.token)
            parsed.token
        }
    }

    override suspend fun sendEvent(event: DesktopSlumpEvent): Result<Unit> {
        val h = host ?: return Result.failure(IllegalStateException("Not paired"))
        val t = token ?: return Result.failure(IllegalStateException("Not paired"))
        return runCatching {
            val response = client.post("http://$h:$port${DesktopLanProtocol.PATH_EVENT}") {
                header(DesktopLanProtocol.HEADER_TOKEN, t)
                contentType(ContentType.Application.Json)
                setBody(DesktopLanJson.eventToJson(event))
            }
            if (response.status.value == 401) {
                error("unauthorized 401")
            }
            check(response.status.isSuccess()) { "Event rejected: ${response.status}" }
        }
    }

    override suspend fun fetchSettings(): Result<DesktopPhoneSettings> {
        val h = host ?: return Result.failure(IllegalStateException("Not paired"))
        val t = token ?: return Result.failure(IllegalStateException("Not paired"))
        return runCatching {
            val response = client.get("http://$h:$port${DesktopLanProtocol.PATH_SETTINGS}") {
                header(DesktopLanProtocol.HEADER_TOKEN, t)
            }
            if (response.status.value == 401) {
                error("unauthorized 401")
            }
            check(response.status.isSuccess()) { "Settings rejected: ${response.status}" }
            DesktopLanJson.parseSettings(response.bodyAsText())
                ?: error("Invalid settings response")
        }
    }

    suspend fun ping(): Result<Unit> {
        val h = host ?: return Result.failure(IllegalStateException("Not paired"))
        return runCatching {
            val response = client.get("http://$h:$port${DesktopLanProtocol.PATH_PING}")
            check(response.status.isSuccess()) { "Ping failed: ${response.status}" }
        }
    }

    override fun clear() {
        host = null
        token = null
        prefs.remove("bridge_host")
        prefs.remove("bridge_token")
        prefs.remove("bridge_port")
    }

    fun close() {
        client.close()
    }
}
