package com.keepstraight.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.ghost.serialization.Ghost
import com.keepstraight.data.UserPreferencesRepository
import com.keepstraight.shared.model.AlertPreferences
import com.keepstraight.shared.model.CalibrationCaptureResult
import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.model.WatchControlCommand
import com.keepstraight.shared.model.WatchControlMessage
import com.keepstraight.shared.sync.CalibrationResultCodec
import com.keepstraight.shared.sync.SyncCapabilities
import com.keepstraight.shared.sync.SyncPaths
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeoutException

class PhoneWearSyncManager(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
) : com.keepstraight.shared.repository.DeviceSyncGateway,
    MessageClient.OnMessageReceivedListener,
    DataClient.OnDataChangedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val messageClient by lazy { Wearable.getMessageClient(context) }
    private val nodeClient by lazy { Wearable.getNodeClient(context) }
    private val dataClient by lazy { Wearable.getDataClient(context) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(context) }

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _calibrationResult = MutableSharedFlow<CalibrationCaptureResult>(extraBufferCapacity = 1)
    override val calibrationResult: SharedFlow<CalibrationCaptureResult> = _calibrationResult.asSharedFlow()

    private val calibrationMutex = Mutex()
    private var calibrationDeferred: CompletableDeferred<CalibrationCaptureResult>? = null

    init {
        messageClient.addListener(this)
        dataClient.addListener(this)
        scope.launch {
            runCatching {
                capabilityClient.addLocalCapability(SyncCapabilities.PHONE).await()
                Log.i(TAG, "Local capability ${SyncCapabilities.PHONE} advertised")
            }.onFailure { error ->
                if (error is com.google.android.gms.common.api.ApiException &&
                    error.statusCode == CAPABILITY_DUPLICATE
                ) {
                    Log.i(TAG, "Local capability ${SyncCapabilities.PHONE} already present")
                } else {
                    Log.w(TAG, "Capability advertise failed", error)
                }
            }
        }

        scope.launch {
            combine(
                userPreferencesRepository.pairedWatchId,
                observeReachableNodes(),
            ) { watchId, nodes ->
                watchId != null && nodes.any { it.id == watchId }
            }.collect { connected ->
                _isConnected.value = connected
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            SyncPaths.PING -> {
                Log.i(
                    TAG,
                    "Ping from watch node=${messageEvent.sourceNodeId} " +
                        "payload=${messageEvent.data.decodeToString()}",
                )
            }
            SyncPaths.CALIBRATE_RESULT -> {
                runCatching {
                    onCalibrationResult(CalibrationResultCodec.decode(messageEvent.data))
                }.onFailure {
                    Log.e(TAG, "Failed decoding calibrate message", it)
                }
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.use { buffer ->
            for (event in buffer) {
                if (event.type != DataEvent.TYPE_CHANGED) continue
                val path = event.dataItem.uri.path ?: continue
                if (path != SyncPaths.CALIBRATE_RESULT) continue
                runCatching {
                    val map = DataMapItem.fromDataItem(event.dataItem).dataMap
                    onCalibrationResult(
                        CalibrationCaptureResult(
                            basePitch = map.getFloat(CalibrationResultCodec.KEY_PITCH),
                            baseRoll = map.getFloat(CalibrationResultCodec.KEY_ROLL),
                            capturedAt = map.getLong(CalibrationResultCodec.KEY_CAPTURED_AT),
                        ),
                    )
                }.onFailure {
                    Log.e(TAG, "Failed reading calibrate DataItem", it)
                }
            }
        }
    }

    override suspend fun discoverPairedDevices(): List<com.keepstraight.shared.repository.PairedDevice> =
        withContext(Dispatchers.IO) {
            awaitWear { nodeClient.connectedNodes.await() }
                .getOrElse { emptyList() }
                .map { node ->
                    com.keepstraight.shared.repository.PairedDevice(
                        id = node.id,
                        displayName = node.displayName,
                    )
                }
        }

    suspend fun discoverWatchNodes(): List<Node> = withContext(Dispatchers.IO) {
        awaitWear { nodeClient.connectedNodes.await() }.getOrDefault(emptyList())
    }

    /** Immediate reachability check (does not wait for the 5s poll). */
    suspend fun refreshConnectionStatus(): Boolean = withContext(Dispatchers.IO) {
        val watchId = userPreferencesRepository.pairedWatchId.first()
        if (watchId == null) {
            _isConnected.value = false
            return@withContext false
        }
        val nodes = awaitWear { nodeClient.connectedNodes.await() }.getOrDefault(emptyList())
        val connected = nodes.any { it.id == watchId }
        _isConnected.value = connected
        connected
    }

    override suspend fun pairDevice(deviceId: String) {
        userPreferencesRepository.setPairedWatchId(
            watchId = deviceId,
            pairedAtMs = System.currentTimeMillis(),
        )
        refreshConnectionStatus()
    }

    override suspend fun clearPairedDevice() {
        userPreferencesRepository.setPairedWatchId(watchId = null, pairedAtMs = null)
        _isConnected.value = false
    }

    override suspend fun sendCalibration(config: PostureCalibrationConfig): Result<Unit> =
        sendToPairedWatch(SyncPaths.CALIBRATION, Ghost.encodeToBytes(config))

    override suspend fun sendControl(command: WatchControlCommand): Result<Unit> {
        val payload = Ghost.encodeToBytes(WatchControlMessage(command))
        // Desktop slump alerts must reach the watch even if pairedWatchId is stale.
        return if (command == WatchControlCommand.TRIGGER_ALERT) {
            broadcastControl(payload)
        } else {
            sendToPairedWatch(SyncPaths.CONTROL, payload)
        }
    }

    private suspend fun broadcastControl(payload: ByteArray): Result<Unit> =
        withContext(Dispatchers.IO) {
            val pairedId = userPreferencesRepository.pairedWatchId.first()
            val nodes = awaitWear { nodeClient.connectedNodes.await() }.getOrElse {
                return@withContext Result.failure(it)
            }
            val capableIds = awaitWear {
                capabilityClient
                    .getCapability(SyncCapabilities.WEAR, CapabilityClient.FILTER_REACHABLE)
                    .await()
                    .nodes
                    .map { it.id }
            }.getOrDefault(emptyList())
            val targets = linkedSetOf<String>().apply {
                addAll(capableIds)
                pairedId?.let { add(it) }
                addAll(nodes.map { it.id })
            }
            if (targets.isEmpty()) {
                Log.w(TAG, "TRIGGER_ALERT: no wear targets")
                return@withContext Result.failure(IllegalStateException(ERROR_UNREACHABLE))
            }
            var anyOk = false
            for (nodeId in targets) {
                val ok = awaitWear {
                    messageClient.sendMessage(nodeId, SyncPaths.CONTROL, payload).await()
                    Unit
                }.isSuccess
                Log.i(TAG, "TRIGGER_ALERT → $nodeId ok=$ok")
                anyOk = anyOk || ok
            }
            if (anyOk) Result.success(Unit)
            else Result.failure(IllegalStateException(ERROR_UNREACHABLE))
        }

    override suspend fun sendPreferences(
        sensitivity: SensitivityLevel,
        alertPreferences: AlertPreferences,
    ): Result<Unit> {
        val prefsResult = sendToPairedWatch(
            SyncPaths.PREFERENCES,
            Ghost.encodeToBytes(alertPreferences),
        )
        if (prefsResult.isFailure) return prefsResult

        val pitch = userPreferencesRepository.calibrationPitch.first()
        val roll = userPreferencesRepository.calibrationRoll.first()
        return if (pitch != null && roll != null) {
            sendCalibration(buildCalibrationConfig(pitch, roll, sensitivity))
        } else {
            Result.success(Unit)
        }
    }

    private suspend fun buildCalibrationConfig(
        pitch: Float,
        roll: Float,
        sensitivity: SensitivityLevel,
    ): PostureCalibrationConfig {
        val hasSlump = userPreferencesRepository.hasSlumpReference.first()
        val slumpPitch = userPreferencesRepository.slumpReferencePitch.first()
        val slumpRoll = userPreferencesRepository.slumpReferenceRoll.first()
        return PostureCalibrationConfig(
            basePitch = pitch,
            baseRoll = roll,
            sensitivity = sensitivity,
            slumpDurationThresholdMs = userPreferencesRepository.slumpDurationThresholdMs.first(),
            repeatAlertIntervalMs = userPreferencesRepository.repeatAlertIntervalMs.first(),
            hasSlumpReference = hasSlump && slumpPitch != null && slumpRoll != null,
            slumpPitch = slumpPitch ?: 0f,
            slumpRoll = slumpRoll ?: 0f,
        )
    }

    override suspend fun requestSync(): Result<Unit> =
        sendToPairedWatch(SyncPaths.SYNC_REQUEST, ByteArray(0))

    override suspend fun requestCalibrationCapture(): Result<Unit> {
        calibrationMutex.withLock {
            calibrationDeferred?.cancel()
            calibrationDeferred = CompletableDeferred()
        }
        // Fan-out Message API to every reachable node + Data Layer.
        // sendMessage "success" only means GMS accepted the send — not that wear handled it.
        val messageResult = broadcastCalibrateRequest()
        val dataResult = publishCalibrateRequest()
        Log.i(
            TAG,
            "Calibrate request done message=${messageResult.isSuccess} data=${dataResult.isSuccess}",
        )
        return when {
            messageResult.isSuccess || dataResult.isSuccess -> Result.success(Unit)
            else -> messageResult
        }
    }

    private suspend fun broadcastCalibrateRequest(): Result<Unit> = withContext(Dispatchers.IO) {
        val pairedId = userPreferencesRepository.pairedWatchId.first()
        val nodes = awaitWear { nodeClient.connectedNodes.await() }.getOrElse {
            return@withContext Result.failure(it)
        }
        Log.i(
            TAG,
            "Calibrate targets paired=$pairedId connected=" +
                nodes.joinToString { "${it.displayName}(${it.id},nearby=${it.isNearby})" },
        )

        val capableIds = awaitWear {
            capabilityClient
                .getCapability(SyncCapabilities.WEAR, CapabilityClient.FILTER_REACHABLE)
                .await()
                .nodes
                .map { it.id }
        }.getOrDefault(emptyList())
        Log.i(TAG, "Capable wear nodes=$capableIds")

        val targets = linkedSetOf<String>().apply {
            addAll(capableIds)
            pairedId?.let { add(it) }
            addAll(nodes.map { it.id })
        }
        if (targets.isEmpty()) {
            return@withContext Result.failure(IllegalStateException(ERROR_UNREACHABLE))
        }

        var anyOk = false
        for (nodeId in targets) {
            val ok = awaitWear {
                messageClient.sendMessage(nodeId, SyncPaths.CALIBRATE_REQUEST, ByteArray(0)).await()
                Unit
            }.isSuccess
            Log.i(TAG, "sendMessage calibrate → $nodeId ok=$ok")
            anyOk = anyOk || ok
        }
        if (anyOk) Result.success(Unit)
        else Result.failure(IllegalStateException(ERROR_UNREACHABLE))
    }

    private suspend fun publishCalibrateRequest(): Result<Unit> = withContext(Dispatchers.IO) {
        awaitWear {
            val request = PutDataMapRequest.create(SyncPaths.CALIBRATE_REQUEST).apply {
                dataMap.putLong(CalibrationResultCodec.KEY_SENT_AT, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            dataClient.putDataItem(request).await()
            Unit
        }
    }

    override suspend fun awaitCalibrationResult(timeoutMs: Long): CalibrationCaptureResult? {
        val deferred = calibrationMutex.withLock { calibrationDeferred } ?: return null
        return withTimeoutOrNull(timeoutMs) { deferred.await() }
    }

    override suspend fun reconnect(): Result<Unit> {
        val watchId = userPreferencesRepository.pairedWatchId.first()
            ?: return Result.failure(IllegalStateException(ERROR_NO_PAIRED))

        val nodesResult = awaitWear { nodeClient.connectedNodes.await() }
        val nodes = nodesResult.getOrElse {
            return Result.failure(it)
        }
        if (nodes.none { it.id == watchId }) {
            _isConnected.value = false
            return Result.failure(IllegalStateException(ERROR_UNREACHABLE))
        }
        _isConnected.value = true

        val resumeResult = sendControl(WatchControlCommand.RESUME_CONNECTION)
        if (resumeResult.isFailure) return resumeResult
        return syncAllPreferences()
    }

    override suspend fun syncAllPreferences(): Result<Unit> {
        val sensitivity = userPreferencesRepository.sensitivity.first()
        val alertPrefs = userPreferencesRepository.alertPreferences.first()
        val monitoring = userPreferencesRepository.monitoringEnabled.first()
        val alerts = userPreferencesRepository.alertsEnabled.first()
        val pitch = userPreferencesRepository.calibrationPitch.first()
        val roll = userPreferencesRepository.calibrationRoll.first()

        sendPreferences(sensitivity, alertPrefs).getOrElse { return Result.failure(it) }
        if (pitch != null && roll != null) {
            sendCalibration(buildCalibrationConfig(pitch, roll, sensitivity))
                .getOrElse { return Result.failure(it) }
        }
        sendControl(
            if (monitoring) WatchControlCommand.START_ALGORITHM else WatchControlCommand.STOP_ALGORITHM,
        ).getOrElse { return Result.failure(it) }
        sendControl(
            if (alerts) WatchControlCommand.RESUME_ALERTS else WatchControlCommand.PAUSE_ALERTS,
        ).getOrElse { return Result.failure(it) }
        return requestSync()
    }

    fun onCalibrationResult(result: CalibrationCaptureResult) {
        Log.i(TAG, "Calibration result received pitch=${result.basePitch} roll=${result.baseRoll}")
        _calibrationResult.tryEmit(result)
        val deferred = calibrationDeferred
        if (deferred != null && deferred.isActive) {
            deferred.complete(result)
        }
    }

    private suspend fun sendToPairedWatch(path: String, payload: ByteArray): Result<Unit> =
        withContext(Dispatchers.IO) {
            val watchId = userPreferencesRepository.pairedWatchId.first()
                ?: return@withContext Result.failure(IllegalStateException(ERROR_NO_PAIRED))
            awaitWear {
                messageClient.sendMessage(watchId, path, payload).await()
                Unit
            }
        }

    private fun observeReachableNodes() = kotlinx.coroutines.flow.flow {
        while (true) {
            val nodes = awaitWear { nodeClient.connectedNodes.await() }.getOrDefault(emptyList())
            emit(nodes)
            kotlinx.coroutines.delay(CONNECTION_POLL_MS)
        }
    }

    private suspend fun <T> awaitWear(
        timeoutMs: Long = WEAR_TIMEOUT_MS,
        block: suspend () -> T,
    ): Result<T> = try {
        Result.success(withTimeout(timeoutMs) { block() })
    } catch (_: TimeoutCancellationException) {
        Result.failure(TimeoutException("Wearable API timed out after ${timeoutMs}ms"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        private const val TAG = "KeepStraightPhone"
        private const val CONNECTION_POLL_MS = 5_000L
        private const val WEAR_TIMEOUT_MS = 8_000L
        private const val CAPABILITY_DUPLICATE = 4006
        const val ERROR_NO_PAIRED = "NO_PAIRED_WATCH"
        const val ERROR_UNREACHABLE = "WATCH_UNREACHABLE"
    }
}
