package com.keepstraight.presentation.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.keepstraight.data.PostureHistoryRepository
import com.keepstraight.data.local.PostureEventEntity
import com.keepstraight.presentation.common.PhonePresentationConfig
import com.keepstraight.shared.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AppShellViewModel(
    userPreferencesRepository: PreferencesRepository,
    postureHistoryRepository: PostureHistoryRepository,
) : ViewModel() {

    val onboardingComplete: StateFlow<Boolean> = userPreferencesRepository.onboardingComplete
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), false)

    val eventsPaged: Flow<PagingData<PostureEventEntity>> =
        postureHistoryRepository.eventsPaged().cachedIn(viewModelScope)
}
