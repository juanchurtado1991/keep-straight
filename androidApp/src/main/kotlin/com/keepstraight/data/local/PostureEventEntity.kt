package com.keepstraight.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "posture_events",
    indices = [Index(value = ["timestamp", "eventType"], unique = true)],
)
data class PostureEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val durationSeconds: Int,
    val timestamp: Long,
)
