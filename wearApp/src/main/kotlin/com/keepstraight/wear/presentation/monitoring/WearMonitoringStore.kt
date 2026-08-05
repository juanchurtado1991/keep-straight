package com.keepstraight.wear.presentation.monitoring

import com.keepstraight.wear.state.MonitoringSession
import kotlinx.coroutines.flow.StateFlow

/** Presentation facade over [MonitoringSession] for Compose UI. */
class WearMonitoringStore(
    private val session: MonitoringSession,
) {
    val monitoringState = session.monitoringState
    val isCalibrating: StateFlow<Boolean> = session.isCalibrating
    val statusText = session.statusText
    val liveDeviationDegrees = session.liveDeviationDegrees
    val liveSlumpScore = session.liveSlumpScore
}
