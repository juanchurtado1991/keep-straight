package com.keepstraight.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepstraight.presentation.common.PhonePresentationConfig
import com.keepstraight.shared.application.phone.CompleteOnboardingUseCase
import com.keepstraight.shared.application.phone.PhoneWatchSettingsUseCase
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.platform.BatteryOptimizationProbe
import com.keepstraight.shared.repository.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val settingsUseCase: PhoneWatchSettingsUseCase,
    userPreferencesRepository: PreferencesRepository,
    private val batteryProbe: BatteryOptimizationProbe,
) : ViewModel() {

    val sensitivity: StateFlow<SensitivityLevel> = userPreferencesRepository.sensitivity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), SensitivityLevel.NORMAL)

    val isCalibrated: StateFlow<Boolean> = combine(
        userPreferencesRepository.calibrationPitch,
        userPreferencesRepository.hasSlumpReference,
    ) { pitch, hasSlump ->
        pitch != null && hasSlump
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS),
        false,
    )

    fun setSensitivity(level: SensitivityLevel) {
        viewModelScope.launch { settingsUseCase.setSensitivity(level) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { completeOnboardingUseCase() }
    }

    fun isBatteryOptimizationEnabled(): Boolean = batteryProbe.isOptimizationRequired()
}
