package com.keepstraight.desktop.bridge

import com.ghost.serialization.ktor.ghost
import com.keepstraight.shared.bridge.BridgeProtocolError
import com.keepstraight.shared.bridge.DesktopLanProtocol
import com.keepstraight.shared.bridge.DesktopPairOffer
import com.keepstraight.shared.bridge.DesktopPairingQr
import com.keepstraight.shared.bridge.PhoneHelloRequest
import com.keepstraight.shared.bridge.PhoneHelloResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
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
        if (hosts.isEmpty()) {
            throw IllegalStateException("No LAN IPv4 address available for QR pairing")
        }
        val offer = DesktopPairOffer(
            hosts = hosts,
            port = DesktopLanProtocol.PAIR_ASSIST_PORT,
            nonce = nonce,
        )
        _offer.value = offer
        server = embeddedServer(
            CIO,
            port = DesktopLanProtocol.PAIR_ASSIST_PORT,
            host = "0.0.0.0",
        ) {
            install(ContentNegotiation) {
                ghost()
            }
            routing {
                post(DesktopLanProtocol.PATH_PHONE_HELLO) {
                    val req = try {
                        call.receive<PhoneHelloRequest>()
                    } catch (_: Exception) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            PhoneHelloResponse(
                                ok = false,
                                errorCode = BridgeProtocolError.INVALID_QR,
                            ),
                        )
                        return@post
                    }
                    val expected = expectedNonce
                    if (expected == null || req.nonce != expected) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            PhoneHelloResponse(
                                ok = false,
                                errorCode = BridgeProtocolError.INVALID_QR,
                            ),
                        )
                        return@post
                    }
                    if (req.protocolVersion != DesktopLanProtocol.VERSION) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            PhoneHelloResponse(
                                ok = false,
                                errorCode = BridgeProtocolError.UPDATE_APP,
                            ),
                        )
                        return@post
                    }
                    val result = onPhoneHello(req)
                    call.respond(
                        if (result.ok) HttpStatusCode.OK else HttpStatusCode.BadRequest,
                        result,
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
