package com.keepstraight.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Per-hour desk time while [PresenceState.SITTING], plus time with good posture
 * (sitting and not slumped).
 */
@Entity(
    tableName = "work_hour_stats",
    indices = [Index(value = ["hourStartMs"], unique = true)],
)
data class WorkHourStatEntity(
    @PrimaryKey val hourStartMs: Long,
    val seatedSeconds: Int,
    val goodPostureSeconds: Int,
)
