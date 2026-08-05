package com.keepstraight.shared.application.phone

import com.keepstraight.shared.repository.DeviceSyncGateway
import com.keepstraight.shared.repository.PreferencesRepository

class PairingUseCase(
    private val preferencesRepository: PreferencesRepository,
    private val deviceSyncGateway: DeviceSyncGateway,
) {
    suspend fun discoverDevices() = deviceSyncGateway.discoverPairedDevices()

    suspend fun pairDevice(deviceId: String) {
        deviceSyncGateway.pairDevice(deviceId)
    }

    suspend fun unpairDevice() {
        deviceSyncGateway.clearPairedDevice()
    }
}
