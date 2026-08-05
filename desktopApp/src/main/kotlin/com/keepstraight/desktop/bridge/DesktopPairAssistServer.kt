package com.keepstraight.desktop.bridge

import com.keepstraight.shared.bridge.BridgeProtocolError
import com.keepstraight.shared.bridge.DesktopLanJson
import com.keepstraight.shared.bridge.DesktopLanProtocol
import com.keepstraight.shared.bridge.DesktopPairOffer
import com.keepstraight.shared.bridge.DesktopPairingQr
import com.keepstraight.shared.bridge.PhoneHelloRequest
import com.keepstraight.shared.bridge.PhoneHelloResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.NetworkInterface
import java.util.UUID

/**
 * Short-lived LAN listener advertised in the desktop QR.
 * Phone POSTs [PATH_PHONE_HELLO]; desktop then pairs as client to the phone ingest server.
 */
class DesktopPairAssistServer(
    private val onPhoneHello: suspend (PhoneHelloRequest) -> PhoneHelloResponse,
) {
    private var server: EmbeddedServer<*, *>? = null
    private var expectedNonce: String? = null

    private val _offer = MutableStateFlow<DesktopPairOffer?>(null)
    val offer: StateFlow<DesktopPairOffer?> = _offer.asStateFlow()

    val isRunning: Boolean get() = server != null

    fun start(): DesktopPairOffer {
        stop()
        val nonce = UUID.randomUUID().toString().replace("-", "").take(16)
        expectedNonce = nonce
        val hosts = discoverIpv4Addresses()
        val offer = DesktopPairOffer(
            hosts = hosts.ifEmpty { listOf("127.0.0.1") },
            port = DesktopLanProtocol.PAIR_ASSIST_PORT,
            nonce = nonce,
        )
        _offer.value = offer
        server = embeddedServer(
            CIO,
            port = DesktopLanProtocol.PAIR_ASSIST_PORT,
            host = "0.0.0.0",
        ) {
            routing {
                post(DesktopLanProtocol.PATH_PHONE_HELLO) {
                    val body = call.receiveText()
                    val req = DesktopLanJson.parsePhoneHello(body)
                    val expected = expectedNonce
                    if (req == null || expected == null || req.nonce != expected) {
                        call.respondText(
                            DesktopLanJson.phoneHelloResponseToJson(
                                PhoneHelloResponse(
                                    ok = false,
                                    errorCode = BridgeProtocolError.INVALID_QR,
                                ),
                            ),
                            ContentType.Application.Json,
                            HttpStatusCode.Unauthorized,
                        )
                        return@post
                    }
                    if (req.protocolVersion != DesktopLanProtocol.VERSION) {
                        call.respondText(
                            DesktopLanJson.phoneHelloResponseToJson(
                                PhoneHelloResponse(
                                    ok = false,
                                    errorCode = BridgeProtocolError.UPDATE_APP,
                                ),
                            ),
                            ContentType.Application.Json,
                            HttpStatusCode.BadRequest,
                        )
                        return@post
                    }
                    val result = onPhoneHello(req)
                    call.respondText(
                        DesktopLanJson.phoneHelloResponseToJson(result),
                        ContentType.Application.Json,
                        if (result.ok) HttpStatusCode.OK else HttpStatusCode.BadRequest,
                    )
                }
            }
        }.also { it.start(wait = false) }
        return offer
    }

    fun stop() {
        runCatching { server?.stop(500, 1000) }
        server = null
        expectedNonce = null
        _offer.value = null
    }

    fun qrPayload(): String? = _offer.value?.let { DesktopPairingQr.encode(it) }

    companion object {
        fun discoverIpv4Addresses(): List<String> {
            return try {
                NetworkInterface.getNetworkInterfaces().toList()
                    .filter { it.isUp && !it.isLoopback }
                    .flatMap { it.inetAddresses.toList() }
                    .filter { !it.isLoopbackAddress && it.hostAddress?.contains(':') != true }
                    .mapNotNull { it.hostAddress }
                    .sortedByDescending { rankLanAddress(it) }
            } catch (_: Exception) {
                emptyList()
            }
        }

        private fun rankLanAddress(ip: String): Int = when {
            ip.startsWith("192.168.") -> 3
            ip.startsWith("10.") -> 2
            ip.startsWith("172.") -> 1
            else -> 0
        }
    }
}
