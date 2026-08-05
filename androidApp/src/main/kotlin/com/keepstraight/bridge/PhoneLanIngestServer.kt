package com.keepstraight.bridge

import android.content.Context
import android.util.Log
import com.keepstraight.R
import com.keepstraight.data.PostureHistoryRepository
import com.keepstraight.data.UserPreferencesRepository
import com.ghost.serialization.ktor.ghost
import com.keepstraight.shared.bridge.BridgeProtocolError
import com.keepstraight.shared.bridge.DesktopLanAckResponse
import com.keepstraight.shared.bridge.DesktopLanPingResponse
import com.keepstraight.shared.bridge.DesktopLanProtocol
import com.keepstraight.shared.bridge.DesktopPairRequest
import com.keepstraight.shared.bridge.DesktopPairResponse
import com.keepstraight.shared.bridge.DesktopPhoneSettings
import com.keepstraight.shared.bridge.DesktopSlumpEvent
import com.keepstraight.shared.bridge.DesktopSlumpEventType
import com.keepstraight.shared.model.PostureEvent
import com.keepstraight.shared.model.PostureEventType
import com.keepstraight.shared.model.WatchControlCommand
import com.keepstraight.sync.PhoneWearSyncManager
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.header
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.net.NetworkInterface
import java.util.UUID
import kotlin.random.Random

/**
 * Phase 2: accepts desktop slump events on the LAN and forwards haptics to the watch.
 * Also serves phone settings (sensitivity/timers) as source of truth for desktop.
 */
class PhoneLanIngestServer(
    private val context: Context,
    private val historyRepository: PostureHistoryRepository,
    private val syncManager: PhoneWearSyncManager,
    private val preferencesRepository: UserPreferencesRepository,
) {
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var server: EmbeddedServer<*, *>? = null

    private val prefs = context.getSharedPreferences(AndroidBridgePrefsKeys.PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var authToken: String? = prefs.getString(AndroidBridgePrefsKeys.TOKEN, null)

    private val pairAttemptLock = Any()
    private var pairingCodeExpiresAtMs = 0L
    private var pairingAttemptCount = 0
    private var pairingAttemptWindowStartMs = 0L

    private val _pairingCode = MutableStateFlow<String?>(null)
    val pairingCode: StateFlow<String?> = _pairingCode.asStateFlow()

    private val _desktopPaired = MutableStateFlow(!authToken.isNullOrBlank())
    val desktopPaired: StateFlow<Boolean> = _desktopPaired.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _localAddresses = MutableStateFlow<List<String>>(emptyList())
    val localAddresses: StateFlow<List<String>> = _localAddresses.asStateFlow()

    var onPairingStateChanged: (() -> Unit)? = null

    fun start() {
        if (server != null) return
        _localAddresses.value = discoverIpv4Addresses()
        server = embeddedServer(CIO, port = DesktopLanProtocol.DEFAULT_PORT, host = "0.0.0.0") {
            install(ContentNegotiation) {
                ghost()
            }
            routing {
                get(DesktopLanProtocol.PATH_PING) {
                    call.respond(
                        DesktopLanPingResponse(
                            ok = true,
                            protocolVersion = DesktopLanProtocol.VERSION,
                        ),
                    )
                }
                get(DesktopLanProtocol.PATH_SETTINGS) {
                    val token = call.request.header(DesktopLanProtocol.HEADER_TOKEN)
                    if (!isAuthorized(token)) {
                        call.respondText(
                            context.getString(R.string.lan_http_unauthorized),
                            status = HttpStatusCode.Unauthorized,
                        )
                        return@get
                    }
                    call.respond(currentPhoneSettings())
                }
                post(DesktopLanProtocol.PATH_PAIR) {
                    val req = try {
                        call.receive<DesktopPairRequest>()
                    } catch (_: Exception) {
                        call.respondPairFailure(BridgeProtocolError.INVALID_CODE)
                        return@post
                    }
                    val expected = _pairingCode.value
                    val now = System.currentTimeMillis()
                    if (expected == null) {
                        call.respondPairFailure(BridgeProtocolError.INVALID_CODE)
                        return@post
                    }
                    if (now > pairingCodeExpiresAtMs) {
                        _pairingCode.value = null
                        call.respondPairFailure(BridgeProtocolError.CODE_EXPIRED)
                        return@post
                    }
                    if (!recordPairAttempt(now)) {
                        call.respondPairFailure(BridgeProtocolError.TOO_MANY_ATTEMPTS)
                        return@post
                    }
                    if (req.code != expected) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            DesktopPairResponse(
                                ok = false,
                                errorCode = BridgeProtocolError.INVALID_CODE,
                            ),
                        )
                        return@post
                    }
                    if (req.protocolVersion != DesktopLanProtocol.VERSION) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            DesktopPairResponse(
                                ok = false,
                                errorCode = BridgeProtocolError.UPDATE_APP,
                            ),
                        )
                        return@post
                    }
                    val token = UUID.randomUUID().toString()
                    authToken = token
                    prefs.edit().putString(AndroidBridgePrefsKeys.TOKEN, token).commit()
                    _pairingCode.value = null
                    pairingCodeExpiresAtMs = 0L
                    pairingAttemptCount = 0
                    _desktopPaired.value = true
                    onPairingStateChanged?.invoke()
                    call.respond(
                        DesktopPairResponse(
                            ok = true,
                            token = token,
                        ),
                    )
                }
                post(DesktopLanProtocol.PATH_EVENT) {
                    val token = call.request.header(DesktopLanProtocol.HEADER_TOKEN)
                    if (!isAuthorized(token)) {
                        call.respondText(
                            context.getString(R.string.lan_http_unauthorized),
                            status = HttpStatusCode.Unauthorized,
                        )
                        return@post
                    }
                    val event = try {
                        call.receive<DesktopSlumpEvent>()
                    } catch (_: Exception) {
                        call.respondText(
                            context.getString(R.string.lan_http_bad_request),
                            status = HttpStatusCode.BadRequest,
                        )
                        return@post
                    }
                    if (event.protocolVersion != DesktopLanProtocol.VERSION) {
                        call.respondText(
                            context.getString(R.string.lan_http_bad_request),
                            status = HttpStatusCode.BadRequest,
                        )
                        return@post
                    }
                    val delivered = handleEvent(event)
                    if (!delivered) {
                        call.respondText(
                            context.getString(R.string.lan_http_watch_unreachable),
                            status = HttpStatusCode.BadGateway,
                        )
                        return@post
                    }
                    call.respond(DesktopLanAckResponse())
                }
            }
        }.also {
            it.start(wait = false)
            _isRunning.value = true
            Log.i(TAG, "Desktop LAN ingest listening on ${DesktopLanProtocol.DEFAULT_PORT}")
        }
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
        _isRunning.value = false
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    fun generatePairingCode(): String {
        val code = Random.nextInt(100000, 999999).toString()
        _pairingCode.value = code
        pairingCodeExpiresAtMs = System.currentTimeMillis() + PAIR_CODE_TTL_MS
        synchronized(pairAttemptLock) {
            pairingAttemptCount = 0
            pairingAttemptWindowStartMs = System.currentTimeMillis()
        }
        return code
    }

    fun refreshAddresses(): List<String> {
        _localAddresses.value = discoverIpv4Addresses()
        return _localAddresses.value
    }

    fun isPairedWithDesktop(): Boolean = _desktopPaired.value

    fun clearPairing() {
        authToken = null
        prefs.edit().remove(AndroidBridgePrefsKeys.TOKEN).commit()
        _pairingCode.value = null
        pairingCodeExpiresAtMs = 0L
        _desktopPaired.value = false
        onPairingStateChanged?.invoke()
    }

    private fun isAuthorized(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        val expected = authToken ?: prefs.getString(AndroidBridgePrefsKeys.TOKEN, null)?.also { authToken = it }
        return token == expected
    }

    private fun recordPairAttempt(nowMs: Long): Boolean {
        synchronized(pairAttemptLock) {
            if (nowMs - pairingAttemptWindowStartMs > PAIR_ATTEMPT_WINDOW_MS) {
                pairingAttemptWindowStartMs = nowMs
                pairingAttemptCount = 0
            }
            pairingAttemptCount++
            return pairingAttemptCount <= MAX_PAIR_ATTEMPTS
        }
    }

    private suspend fun ApplicationCall.respondPairFailure(error: BridgeProtocolError) {
        respond(
            HttpStatusCode.Unauthorized,
            DesktopPairResponse(ok = false, errorCode = error),
        )
    }

    /** @return false when a slump alert was required but could not reach the watch. */
    private suspend fun handleEvent(event: DesktopSlumpEvent): Boolean {
        when (event.type) {
            DesktopSlumpEventType.WORK_SAMPLE -> {
                historyRepository.addWorkSample(
                    seatedDeltaSec = event.seatedDeltaSec,
                    goodPostureDeltaSec = event.goodPostureDeltaSec,
                    atMs = normalizeTimestampMs(event.timestampMs),
                )
                return true
            }
            DesktopSlumpEventType.SLUMP_INITIAL,
            DesktopSlumpEventType.SLUMP_REPEAT,
            -> {
                historyRepository.insertEvent(
                    PostureEvent(
                        eventType = PostureEventType.SLUMP_DETECTED,
                        durationSeconds = 0,
                        timestamp = normalizeTimestampMs(event.timestampMs),
                    ),
                )
                val alertsOn = preferencesRepository.alertsEnabled.first()
                if (!alertsOn) {
                    Log.i(TAG, "Skipping watch alert — phone Alerts toggle is off")
                    return true
                }
                val alertPrefs = preferencesRepository.alertPreferences.first()
                if (alertPrefs.phoneNotificationEnabled) {
                    (context.applicationContext as? com.keepstraight.KeepStraightApp)
                        ?.notificationManager
                        ?.showSlumpAlert(0)
                }
                return syncManager.sendControl(WatchControlCommand.TRIGGER_ALERT)
                    .also { sent ->
                        sent.onSuccess {
                            Log.i(TAG, "Forwarded ${event.type} → watch TRIGGER_ALERT")
                        }.onFailure { err ->
                            Log.w(TAG, "Failed to forward ${event.type} to watch: ${err.message}")
                        }
                    }
                    .isSuccess
            }
            else -> return true
        }
    }

    private suspend fun currentPhoneSettings(): DesktopPhoneSettings =
        DesktopPhoneSettings(
            sensitivity = preferencesRepository.sensitivity.first(),
            slumpDurationThresholdMs = preferencesRepository.slumpDurationThresholdMs.first(),
            repeatAlertIntervalMs = preferencesRepository.repeatAlertIntervalMs.first(),
            alertsEnabled = preferencesRepository.alertsEnabled.first(),
        )

    private fun normalizeTimestampMs(raw: Long): Long {
        val now = System.currentTimeMillis()
        return raw.takeIf { it in 1..now + CLOCK_SKEW_MS } ?: now
    }

    private fun discoverIpv4Addresses(): List<String> {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .filter { !it.isLoopbackAddress && it.hostAddress?.contains(':') != true }
                .mapNotNull { it.hostAddress }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val TAG = "PhoneLanIngest"
        private const val PAIR_CODE_TTL_MS = 5 * 60 * 1000L
        private const val PAIR_ATTEMPT_WINDOW_MS = 60 * 1000L
        private const val MAX_PAIR_ATTEMPTS = 10
        private const val CLOCK_SKEW_MS = 60_000L
    }
}
