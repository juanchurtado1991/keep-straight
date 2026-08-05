package com.keepstraight.data.model

import com.keepstraight.data.local.WorkHourStatEntity
import java.time.LocalDate

data class DashboardDayStats(
    val day: LocalDate,
    val hours: List<WorkHourStatEntity>,
    val seatedSeconds: Int,
    val goodPostureSeconds: Int,
    /** True for calendar today — totals are partial and keep growing. */
    val inProgress: Boolean = false,
) {
    val goodRatio: Float
        get() = if (seatedSeconds <= 0) 0f else goodPostureSeconds.toFloat() / seatedSeconds
}
