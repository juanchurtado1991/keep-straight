package com.keepstraight.shared.model

import com.ghost.serialization.annotations.GhostSerialization

@GhostSerialization
data class PostureEventBatch(
    val events: List<PostureEvent>,
)
