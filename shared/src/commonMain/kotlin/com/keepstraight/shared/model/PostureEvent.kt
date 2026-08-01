package com.keepstraight.shared.model

import com.ghost.serialization.annotations.GhostSerialization

@GhostSerialization
data class PostureEvent(
    val eventType: PostureEventType,
    val durationSeconds: Int,
    val timestamp: Long,
)
