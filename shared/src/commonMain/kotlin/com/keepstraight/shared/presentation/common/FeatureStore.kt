package com.keepstraight.shared.presentation.common

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * UDF contract for presentation layers across phone, desktop, and wear.
 */
interface FeatureStore<S, E, F> {
    val state: StateFlow<S>
    val effects: SharedFlow<F>
    fun onEvent(event: E)
}
