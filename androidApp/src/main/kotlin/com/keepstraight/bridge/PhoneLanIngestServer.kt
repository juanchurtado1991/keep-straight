package com.keepstraight.bridge

import android.content.Context
import android.util.Log
import com.keepstraight.data.PostureHistoryRepository
import com.keepstraight.data.UserPreferencesRepository
import com.keepstraight.shared.bridge.DesktopLanJson
import com.keepstraight.shared.bridge.DesktopLanProtocol
import com.keepstraight.shared.bridge.DesktopPairResponse
import com.keepstraight.shared.bridge.DesktopPhoneSettings
import com.keepstraight.shared.bridge.DesktopSlumpEvent
import com.keepstraight.shared.bridge.DesktopSlumpEventType
import com.keepstraight.shared.model.PostureEvent
import com.keepstraight.shared.model.PostureEventType
import com.keepstraight.shared.model.WatchControlCommand
import com.keepstraight.sync.PhoneWearSyncManager
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.header
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var server: EmbeddedServer<*, *>? = null

    private val prefs = context.getSharedPreferences("desktop_bridge", Context.MODE_PRIVATE)

    private val _pairingCode = MutableStateFlow<String?>(null)
    val pairingCode: StateFlow<String?> = _pairingCode.asStateFlow()

    private val _desktopPaired = MutableStateFlow(!prefs.getString("token", null).isNullOrBlank())
    val desktopPaired: StateFlow<Boolean> = _desktopPaired.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _localAddresses = MutableStateFlow<List<String>>(emptyList())
    val localAddresses: StateFlow<List<String>> = _localAddresses.asStateFlow()

    fun start() {
        if (server != null) return
        _localAddresses.value = discoverIpv4Addresses()
        server = embeddedServer(CIO, port = DesktopLanProtocol.DEFAULT_PORT, host = "0.0.0.0") {
            routing {
                get(DesktopLanProtocol.PATH_PING) {
                    call.respondText(
                        """{"ok":true,"protocolVersion":${DesktopLanProtocol.VERSION}}""",
                        ContentType.Application.Json,
                    )
                }
                get(DesktopLanProtocol.PATH_SETTINGS) {
                    val token = call.request.header(DesktopLanProtocol.HEADER_TOKEN)
                    val expected = prefs.getString("token", null)
                    if (token.isNullOrBlank() || token != expected) {
                        call.respondText("unauthorized", status = HttpStatusCode.Unauthorized)
                        return@get
                    }
                    val settings = currentPhoneSettings()
                    call.respondText(
                        DesktopLanJson.settingsToJson(settings),
                        ContentType.Application.Json,
                    )
                }
                post(DesktopLanProtocol.PATH_PAIR) {
                    val body = call.receiveText()
                    val req = DesktopLanJson.parsePairRequest(body)
                    val expected = _pairingCode.value
                    if (req == null || expected == null || req.code != expected) {
                        call.respondText(
                            DesktopLanJson.pairResponseToJson(
                                DesktopPairResponse(ok = false, message = "Invalid code"),
                            ),
                            ContentType.Application.Json,
                            HttpStatusCode.Unauthorized,
                        )
                        return@post
                    }
                    if (req.protocolVersion != DesktopLanProtocol.VERSION) {
                        call.respondText(
                            DesktopLanJson.pairResponseToJson(
                                DesktopPairResponse(ok = false, message = "Update KeepStraight"),
                            ),
                            ContentType.Application.Json,
                            HttpStatusCode.BadRequest,
                        )
                        return@post
                    }
                    val token = UUID.randomUUID().toString()
                    prefs.edit().putString("token", token).apply()
                    _pairingCode.value = null
                    _desktopPaired.value = true
                    call.respondText(
                        DesktopLanJson.pairResponseToJson(
                            DesktopPairResponse(ok = true, token = token, message = "Paired"),
                        ),
                        ContentType.Application.Json,
                    )
                }
                post(DesktopLanProtocol.PATH_EVENT) {
                    val token = call.request.header(DesktopLanProtocol.HEADER_TOKEN)
                    val expected = prefs.getString("token", null)
                    if (token.isNullOrBlank() || token != expected) {
                        call.respondText("unauthorized", status = HttpStatusCode.Unauthorized)
                        return@post
                    }
                    val event = DesktopLanJson.parseEvent(call.receiveText())
                    if (event == null || event.protocolVersion != DesktopLanProtocol.VERSION) {
                        call.respondText("bad request", status = HttpStatusCode.BadRequest)
                        return@post
                    }
                    scope.launch { handleEvent(event) }
                    call.respondText("""{"ok":true}""", ContentType.Application.Json)
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
    }

    fun generatePairingCode(): String {
        val code = Random.nextInt(100000, 999999).toString()
        _pairingCode.value = code
        return code
    }

    fun refreshAddresses(): List<String> {
        _localAddresses.value = discoverIpv4Addresses()
        return _localAddresses.value
    }

    fun isPairedWithDesktop(): Boolean = _desktopPaired.value

    fun clearPairing() {
        prefs.edit().remove("token").apply()
        _pairingCode.value = null
        _desktopPaired.value = false
    }

    private suspend fun handleEvent(event: DesktopSlumpEvent) {
        when (event.type) {
            DesktopSlumpEventType.WORK_SAMPLE -> {
                historyRepository.addWorkSample(
                    seatedDeltaSec = event.seatedDeltaSec,
                    goodPostureDeltaSec = event.goodPostureDeltaSec,
                    atMs = event.timestampMs.takeIf { it > 0 } ?: System.currentTimeMillis(),
                )
            }
            DesktopSlumpEventType.SLUMP_INITIAL,
            DesktopSlumpEventType.SLUMP_REPEAT,
            -> {
                historyRepository.insertEvent(
                    PostureEvent(
                        eventType = PostureEventType.SLUMP_DETECTED,
                        durationSeconds = 0,
                        timestamp = event.timestampMs,
                    ),
                )
                val alertsOn = preferencesRepository.alertsEnabled.first()
                if (!alertsOn) {
                    Log.i(TAG, "Skipping watch alert — phone Alerts toggle is off")
                    return
                }
                val alertPrefs = preferencesRepository.alertPreferences.first()
                if (alertPrefs.phoneNotificationEnabled) {
                    (context.applicationContext as? com.keepstraight.KeepStraightApp)
                        ?.notificationManager
                        ?.showSlumpAlert(0)
                }
                val sent = syncManager.sendControl(WatchControlCommand.TRIGGER_ALERT)
                sent.onSuccess {
                    Log.i(TAG, "Forwarded ${event.type} → watch TRIGGER_ALERT")
                }.onFailure { err ->
                    Log.w(TAG, "Failed to forward ${event.type} to watch: ${err.message}")
                }
            }
            else -> Unit
        }
    }

    private suspend fun currentPhoneSettings(): DesktopPhoneSettings =
        DesktopPhoneSettings(
            sensitivity = preferencesRepository.sensitivity.first().name,
            slumpDurationThresholdMs = preferencesRepository.slumpDurationThresholdMs.first(),
            repeatAlertIntervalMs = preferencesRepository.repeatAlertIntervalMs.first(),
            alertsEnabled = preferencesRepository.alertsEnabled.first(),
        )

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
    }
}
