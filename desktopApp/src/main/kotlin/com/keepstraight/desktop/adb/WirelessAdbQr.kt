package com.keepstraight.desktop.adb

import com.keepstraight.desktop.presentation.DesktopMessageKey
import androidx.compose.ui.graphics.ImageBitmap
import com.keepstraight.desktop.ui.QrCodeBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.atomic.AtomicReference
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

data class WirelessPairOffer(
    val serviceName: String,
    val password: String,
    val qrPayload: String,
    val qrBitmap: ImageBitmap,
)

data class DiscoveredEndpoint(
    val host: String,
    val port: Int,
)

/**
 * Android Studio–style wireless ADB: desktop shows QR, phone scans it
 * (Developer options → Wireless debugging → Pair device with QR code).
 */
object WirelessAdbQr {
    private const val PAIRING_TYPE = "_adb-tls-pairing._tcp.local."
    private const val CONNECT_TYPE = "_adb-tls-connect._tcp.local."

    fun createOffer(): WirelessPairOffer {
        val name = "studio-" + randomToken(10)
        val password = randomDigits(6)
        val payload = "WIFI:T:ADB;S:$name;P:$password;;"
        return WirelessPairOffer(
            serviceName = name,
            password = password,
            qrPayload = payload,
            qrBitmap = QrCodeBitmap.encode(payload, sizePx = 420),
        )
    }

    /**
     * Wait for the phone to advertise pairing for [serviceName], then return host:port.
     */
    suspend fun awaitPairingEndpoint(
        serviceName: String,
        timeoutMs: Long = 90_000L,
    ): AdbResult<DiscoveredEndpoint> = withContext(Dispatchers.IO) {
        discover(PAIRING_TYPE, serviceName, timeoutMs)
            ?: AdbResult.Err(
                AdbErrorKind.PAIR_FAILED,
                titleKey = DesktopMessageKey.ADB_QR_WAITING_PHONE_TITLE,
                bodyKey = DesktopMessageKey.ADB_QR_WAITING_PHONE_BODY,
            )
    }

    /**
     * Any device advertising pairing, regardless of service name. Used for screens where the user
     * types a code instead of scanning (a watch has no camera, so it never gets our QR name).
     */
    suspend fun findAnyPairingEndpoint(
        timeoutMs: Long = 20_000L,
    ): DiscoveredEndpoint? = withContext(Dispatchers.IO) {
        (discover(PAIRING_TYPE, serviceNameFilter = null, timeoutMs) as? AdbResult.Ok)?.value
    }

    /** Devices with wireless debugging already on (no pairing dialog open). */
    suspend fun findAnyConnectEndpoint(
        timeoutMs: Long = 20_000L,
    ): DiscoveredEndpoint? = withContext(Dispatchers.IO) {
        (discover(CONNECT_TYPE, serviceNameFilter = null, timeoutMs) as? AdbResult.Ok)?.value
    }

    suspend fun awaitConnectEndpoint(
        preferredHost: String? = null,
        timeoutMs: Long = 45_000L,
    ): AdbResult<DiscoveredEndpoint> = withContext(Dispatchers.IO) {
        val any = discover(CONNECT_TYPE, serviceNameFilter = null, timeoutMs, preferredHost)
        any ?: AdbResult.Err(
            AdbErrorKind.CONNECT_FAILED,
            titleKey = DesktopMessageKey.ADB_QR_CONNECT_FAILED_TITLE,
            bodyKey = DesktopMessageKey.ADB_QR_CONNECT_FAILED_BODY,
        )
    }

    private suspend fun discover(
        type: String,
        serviceNameFilter: String?,
        timeoutMs: Long,
        preferredHost: String? = null,
    ): AdbResult<DiscoveredEndpoint>? {
        val found = AtomicReference<DiscoveredEndpoint?>(null)
        var jmdns: JmDNS? = null
        return try {
            jmdns = JmDNS.create(lanAddress())
            val listener = object : ServiceListener {
                override fun serviceAdded(event: ServiceEvent) {
                    jmdns?.requestServiceInfo(event.type, event.name, true)
                }

                override fun serviceRemoved(event: ServiceEvent) = Unit

                override fun serviceResolved(event: ServiceEvent) {
                    val info = event.info ?: return
                    val nameOk = serviceNameFilter == null ||
                        info.name.equals(serviceNameFilter, ignoreCase = true) ||
                        info.name.startsWith(serviceNameFilter, ignoreCase = true)
                    if (!nameOk) return
                    val host = info.inetAddresses.firstOrNull()?.hostAddress ?: return
                    val endpoint = DiscoveredEndpoint(host, info.port)
                    if (preferredHost != null) {
                        if (host == preferredHost) {
                            found.set(endpoint)
                        }
                        // Ignore other hosts when we have a preferred IP.
                        return
                    }
                    found.compareAndSet(null, endpoint)
                }
            }
            jmdns.addServiceListener(type, listener)
            val deadline = System.currentTimeMillis() + timeoutMs
            while (coroutineContext.isActive && System.currentTimeMillis() < deadline) {
                found.get()?.let { return AdbResult.Ok(it) }
                delay(200)
            }
            found.get()?.let { AdbResult.Ok(it) }
        } catch (e: Exception) {
            AdbResult.Err(
                AdbErrorKind.UNKNOWN,
                titleKey = DesktopMessageKey.ADB_QR_BROWSE_FAILED_TITLE,
                bodyKey = DesktopMessageKey.ADB_QR_BROWSE_FAILED_BODY,
            )
        } finally {
            runCatching { jmdns?.close() }
        }
    }

    private fun lanAddress(): InetAddress {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it is java.net.Inet4Address }
                ?: InetAddress.getLocalHost()
        } catch (_: Exception) {
            InetAddress.getLocalHost()
        }
    }

    private fun randomToken(len: Int): String {
        val alphabet = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString(len) {
            repeat(len) { append(alphabet[Random.nextInt(alphabet.length)]) }
        }
    }

    private fun randomDigits(len: Int): String =
        buildString(len) { repeat(len) { append(Random.nextInt(10)) } }
}

/**
 * Full flow: show QR → pair → connect → ready to install.
 */
suspend fun AdbInstaller.pairConnectViaQr(
    offer: WirelessPairOffer,
    onStatus: (DesktopMessageKey) -> Unit = {},
): AdbResult<String> {
    onStatus(DesktopMessageKey.ADB_QR_WAITING)
    val pairing = when (val r = WirelessAdbQr.awaitPairingEndpoint(offer.serviceName)) {
        is AdbResult.Ok -> r.value
        is AdbResult.Err -> return r
    }
    onStatus(DesktopMessageKey.ADB_QR_PHONE_FOUND)
    when (val r = pairWireless(pairing.host, pairing.port, offer.password)) {
        is AdbResult.Ok -> Unit
        is AdbResult.Err -> return r
    }
    onStatus(DesktopMessageKey.ADB_CONNECTING_INSTALL)
    val connect = when (
        val r = WirelessAdbQr.awaitConnectEndpoint(preferredHost = pairing.host)
    ) {
        is AdbResult.Ok -> r.value
        is AdbResult.Err -> {
            return r
        }
    }
    when (val r = connectWireless(connect.host, connect.port)) {
        is AdbResult.Ok -> Unit
        is AdbResult.Err -> return r
    }
    onStatus(DesktopMessageKey.ADB_QR_CONNECTED)
    return AdbResult.Ok("${connect.host}:${connect.port}")
}

/**
 * Manual path for devices that can't scan a QR (watches have no camera). Independent from
 * KeepStraight phone linking — this is only wireless debugging for installing the APK.
 */
suspend fun AdbInstaller.pairOrConnect(
    host: String,
    port: Int,
    pairingCode: String?,
    onStatus: (DesktopMessageKey) -> Unit = {},
): AdbResult<String> {
    if (pairingCode.isNullOrBlank()) {
        onStatus(DesktopMessageKey.ADB_CONNECTING_WATCH)
        return when (val r = connectWireless(host, port)) {
            is AdbResult.Ok -> AdbResult.Ok("$host:$port")
            is AdbResult.Err -> r
        }
    }

    onStatus(DesktopMessageKey.ADB_PAIRING_WATCH)
    when (val r = pairWireless(host, port, pairingCode)) {
        is AdbResult.Ok -> Unit
        is AdbResult.Err -> return r
    }
    onStatus(DesktopMessageKey.ADB_PAIRED_FINDING_PORT)
    val connect = when (val r = WirelessAdbQr.awaitConnectEndpoint(preferredHost = host)) {
        is AdbResult.Ok -> r.value
        is AdbResult.Err -> return AdbResult.Err(
            AdbErrorKind.CONNECT_FAILED,
            titleKey = DesktopMessageKey.ADB_WATCH_DEBUG_PORT_TITLE,
            bodyKey = DesktopMessageKey.ADB_WATCH_DEBUG_PORT_BODY,
        )
    }
    when (val r = connectWireless(connect.host, connect.port)) {
        is AdbResult.Ok -> Unit
        is AdbResult.Err -> return r
    }
    onStatus(DesktopMessageKey.ADB_QR_CONNECTED)
    return AdbResult.Ok("${connect.host}:${connect.port}")
}
