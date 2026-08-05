package com.keepstraight.shared.application.phone

import com.keepstraight.shared.repository.DeviceSyncGateway

class ReconnectWatchUseCase(
    private val deviceSyncGateway: DeviceSyncGateway,
) {
    suspend operator fun invoke(): Result<Unit> = deviceSyncGateway.reconnect()
}
