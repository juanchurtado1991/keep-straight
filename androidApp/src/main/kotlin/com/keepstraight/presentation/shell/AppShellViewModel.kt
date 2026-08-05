package com.keepstraight.presentation.shell

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.keepstraight.KeepStraightApp
import com.keepstraight.presentation.common.PhonePresentationConfig
import com.keepstraight.data.local.PostureEventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AppShellViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val app = application as KeepStraightApp

    val onboardingComplete: StateFlow<Boolean> = app.userPreferencesRepository.onboardingComplete
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PhonePresentationConfig.STATE_SUBSCRIPTION_MS), false)

    val eventsPaged: Flow<PagingData<PostureEventEntity>> =
        app.postureHistoryRepository.eventsPaged().cachedIn(viewModelScope)
}
