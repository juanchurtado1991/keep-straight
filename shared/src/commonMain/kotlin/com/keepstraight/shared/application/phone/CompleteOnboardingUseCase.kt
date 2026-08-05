package com.keepstraight.shared.application.phone

import com.keepstraight.shared.repository.DeviceSyncGateway
import com.keepstraight.shared.repository.PreferencesRepository

class CompleteOnboardingUseCase(
    private val preferencesRepository: PreferencesRepository,
    private val deviceSyncGateway: DeviceSyncGateway,
) {
    suspend operator fun invoke() {
        preferencesRepository.setOnboardingComplete(true)
        runCatching { deviceSyncGateway.syncAllPreferences() }
    }
}
