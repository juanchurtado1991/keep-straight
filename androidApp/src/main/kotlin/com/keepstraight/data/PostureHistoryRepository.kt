package com.keepstraight.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.keepstraight.data.local.PostureDatabase
import com.keepstraight.data.local.PostureEventEntity
import com.keepstraight.shared.model.PostureEvent
import com.keepstraight.shared.model.PostureEventType
import kotlinx.coroutines.flow.Flow

class PostureHistoryRepository(
    private val database: PostureDatabase,
) : com.keepstraight.shared.repository.PostureHistoryStore {
    private val dao = database.postureEventDao()

    fun eventsPaged(pageSize: Int = 30): Flow<PagingData<PostureEventEntity>> =
        Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
            pagingSourceFactory = { dao.pagingSource() },
        ).flow

    override suspend fun insertEvent(event: PostureEvent) {
        dao.insert(event.toEntity())
    }

    override suspend fun insertEvents(events: List<PostureEvent>) {
        if (events.isEmpty()) return
        dao.insertAll(events.map { it.toEntity() })
    }

    suspend fun eventCount(): Int = dao.count()

    private fun PostureEvent.toEntity() = PostureEventEntity(
        eventType = eventType.name,
        durationSeconds = durationSeconds,
        timestamp = timestamp,
    )
}
