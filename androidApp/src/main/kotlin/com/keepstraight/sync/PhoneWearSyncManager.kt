package com.keepstraight.sync

import android.content.Context
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.ghost.serialization.Ghost
import com.keepstraight.data.UserPreferencesRepository
import com.keepstraight.shared.model.AlertPreferences
import com.keepstraight.shared.model.CalibrationCaptureResult
import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.model.WatchControlCommand
import com.keepstraight.shared.model.WatchControlMessage
import com.keepstraight.shared.sync.SyncPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PhoneWearSyncManager(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
) : com.keepstraight.shared.repository.DeviceSyncGateway {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val messageClient by lazy { Wearable.getMessageClient(context) }
    private val nodeClient by lazy { Wearable.getNodeClient(context) }

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _calibrationResult = MutableSharedFlow<CalibrationCaptureResult>(extraBufferCapacity = 1)
    override val calibrationResult: SharedFlow<CalibrationCaptureResult> = _calibrationResult.asSharedFlow()

    init {
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

    override suspend fun discoverPairedDevices(): List<com.keepstraight.shared.repository.PairedDevice> =
        withContext(Dispatchers.IO) {
            nodeClient.connectedNodes.await().map { node ->
                com.keepstraight.shared.repository.PairedDevice(
                    id = node.id,
                    displayName = node.displayName,
                )
            }
        }

    suspend fun discoverWatchNodes(): List<Node> = withContext(Dispatchers.IO) {
        nodeClient.connectedNodes.await()
    }

    override suspend fun pairDevice(deviceId: String) {
        userPreferencesRepository.setPairedWatchId(deviceId)
    }

    override suspend fun clearPairedDevice() {
        userPreferencesRepository.setPairedWatchId(null)
    }

    override suspend fun sendCalibration(config: PostureCalibrationConfig): Result<Unit> =
        sendToPairedWatch(SyncPaths.CALIBRATION, Ghost.encodeToBytes(config))

    override suspend fun sendControl(command: WatchControlCommand): Result<Unit> =
        sendToPairedWatch(
            SyncPaths.CONTROL,
            Ghost.encodeToBytes(WatchControlMessage(command)),
        )

    override suspend fun sendPreferences(
        sensitivity: SensitivityLevel,
        alertPreferences: AlertPreferences,
    ): Result<Unit> {
        sendToPairedWatch(SyncPaths.PREFERENCES, Ghost.encodeToBytes(alertPreferences))
        val pitch = userPreferencesRepository.calibrationPitch.first()
        val roll = userPreferencesRepository.calibrationRoll.first()
        return if (pitch != null && roll != null) {
            sendCalibration(
                PostureCalibrationConfig(
                    basePitch = pitch,
                    baseRoll = roll,
                    sensitivity = sensitivity,
                ),
            )
        } else {
            Result.success(Unit)
        }
    }

    override suspend fun requestSync(): Result<Unit> =
        sendToPairedWatch(SyncPaths.SYNC_REQUEST, ByteArray(0))

    override suspend fun requestCalibrationCapture(): Result<Unit> =
        sendControl(WatchControlCommand.CALIBRATE_CAPTURE)

    override suspend fun reconnect(): Result<Unit> {
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
        sendPreferences(sensitivity, alertPrefs)
        if (pitch != null && roll != null) {
            sendCalibration(
                PostureCalibrationConfig(
                    basePitch = pitch,
                    baseRoll = roll,
                    sensitivity = sensitivity,
                ),
            )
        }
        sendControl(
            if (monitoring) WatchControlCommand.START_ALGORITHM else WatchControlCommand.STOP_ALGORITHM,
        )
        sendControl(
            if (alerts) WatchControlCommand.RESUME_ALERTS else WatchControlCommand.PAUSE_ALERTS,
        )
        return requestSync()
    }

    fun onCalibrationResult(result: CalibrationCaptureResult) {
        _calibrationResult.tryEmit(result)
    }

    private suspend fun sendToPairedWatch(path: String, payload: ByteArray): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val watchId = userPreferencesRepository.pairedWatchId.first()
                    ?: error("No paired watch configured")
                messageClient.sendMessage(watchId, path, payload).await()
                Unit
            }
        }

    private fun observeReachableNodes() = kotlinx.coroutines.flow.flow {
        while (true) {
            val nodes = runCatching { nodeClient.connectedNodes.await() }.getOrDefault(emptyList())
            emit(nodes)
            kotlinx.coroutines.delay(CONNECTION_POLL_MS)
        }
    }

    companion object {
        private const val CONNECTION_POLL_MS = 5_000L
    }
}
