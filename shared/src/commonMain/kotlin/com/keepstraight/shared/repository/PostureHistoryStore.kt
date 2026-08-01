package com.keepstraight.shared.repository

import com.keepstraight.shared.model.PostureEvent

interface PostureHistoryStore {
    suspend fun insertEvent(event: PostureEvent)
    suspend fun insertEvents(events: List<PostureEvent>)
}
