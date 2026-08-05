package com.keepstraight.presentation.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keepstraight.KeepStraightApp
import com.keepstraight.shared.application.phone.CompleteOnboardingUseCase
import com.keepstraight.shared.application.phone.PhoneWatchSettingsUseCase
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.util.AndroidBatteryOptimizationProbe
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OnboardingViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val app = application as KeepStraightApp
    private val completeOnboardingUseCase = CompleteOnboardingUseCase(
        app.userPreferencesRepository,
        app.syncManager,
    )
    private val settingsUseCase = PhoneWatchSettingsUseCase(
        app.userPreferencesRepository,
        app.syncManager,
    )
    private val batteryProbe = AndroidBatteryOptimizationProbe(application)

    val sensitivity: StateFlow<SensitivityLevel> = app.userPreferencesRepository.sensitivity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SensitivityLevel.NORMAL)

    fun setSensitivity(level: SensitivityLevel) {
        viewModelScope.launch { settingsUseCase.setSensitivity(level) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { completeOnboardingUseCase() }
    }

    fun isBatteryOptimizationEnabled(): Boolean = batteryProbe.isOptimizationRequired()
}
