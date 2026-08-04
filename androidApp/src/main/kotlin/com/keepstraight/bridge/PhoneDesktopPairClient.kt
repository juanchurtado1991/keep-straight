package com.keepstraight.bridge

import android.util.Log
import com.keepstraight.shared.bridge.DesktopLanJson
import com.keepstraight.shared.bridge.DesktopLanProtocol
import com.keepstraight.shared.bridge.DesktopPairOffer
import com.keepstraight.shared.bridge.PhoneHelloRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhoneDesktopPairClient(
    private val lanIngestServer: PhoneLanIngestServer,
) {
    private val client = HttpClient(CIO)

    suspend fun pairByScanningDesktopQr(offer: DesktopPairOffer): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                lanIngestServer.start()
                val code = lanIngestServer.generatePairingCode()
                val phoneHosts = lanIngestServer.refreshAddresses()
                if (phoneHosts.isEmpty()) {
                    error("No Wi‑Fi address on this phone. Join the same network as your computer.")
                }
                val hello = PhoneHelloRequest(
                    nonce = offer.nonce,
                    code = code,
                    phoneHosts = phoneHosts,
                    phonePort = DesktopLanProtocol.DEFAULT_PORT,
                )
                var lastError: String? = null
                for (desktopHost in offer.hosts) {
                    val ok = postHello(desktopHost, offer.port, hello)
                    if (ok.isSuccess) {
                        Log.i(TAG, "Desktop pair OK via $desktopHost")
                        return@runCatching
                    }
                    lastError = ok.exceptionOrNull()?.message
                    Log.w(TAG, "Hello to $desktopHost failed: $lastError")
                }
                error(lastError ?: "Could not reach desktop. Same Wi‑Fi? QR still open?")
            }
        }

    private suspend fun postHello(
        desktopHost: String,
        port: Int,
        hello: PhoneHelloRequest,
    ): Result<Unit> = runCatching {
        val response = client.post(
            "http://$desktopHost:$port${DesktopLanProtocol.PATH_PHONE_HELLO}",
        ) {
            contentType(ContentType.Application.Json)
            setBody(DesktopLanJson.phoneHelloToJson(hello))
        }
        val body = response.bodyAsText()
        val parsed = DesktopLanJson.parsePhoneHelloResponse(body)
        if (!response.status.isSuccess() || !parsed.ok) {
            error(parsed.message.ifBlank { "Desktop rejected pairing (${response.status.value})" })
        }
    }

    fun close() {
        client.close()
    }

    private companion object {
        const val TAG = "PhoneDesktopPair"
    }
}
