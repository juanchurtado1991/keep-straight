package com.keepstraight.shared.application.phone

import com.keepstraight.shared.repository.DeviceSyncGateway

class RefreshWatchConnectionUseCase(
    private val deviceSyncGateway: DeviceSyncGateway,
) {
    suspend operator fun invoke(): Boolean = deviceSyncGateway.refreshConnectionStatus()
}
