package com.keepstraight.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.keepstraight.data.local.PostureDatabase
import com.keepstraight.data.local.PostureEventEntity
import com.keepstraight.data.local.WorkHourStatEntity
import com.keepstraight.shared.model.PostureEvent
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PostureHistoryRepository(
    private val database: PostureDatabase,
) : com.keepstraight.shared.repository.PostureHistoryStore {
    private val dao = database.postureEventDao()
    private val workHourDao = database.workHourStatDao()

    fun eventsPaged(pageSize: Int = 30): Flow<PagingData<PostureEventEntity>> =
        Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
            pagingSourceFactory = { dao.pagingSource() },
        ).flow

    fun workStatsFrom(fromMs: Long): Flow<List<WorkHourStatEntity>> =
        workHourDao.observeFrom(fromMs)

    override suspend fun insertEvent(event: PostureEvent) {
        dao.insert(event.toEntity())
    }

    override suspend fun insertEvents(events: List<PostureEvent>) {
        if (events.isEmpty()) return
        dao.insertAll(events.map { it.toEntity() })
    }

    suspend fun addWorkSample(seatedDeltaSec: Int, goodPostureDeltaSec: Int, atMs: Long) {
        val hour = WorkHourStatsMockSeeder.hourBucketStart(atMs)
        workHourDao.addDelta(hour, seatedDeltaSec, goodPostureDeltaSec)
    }

    /** One-shot: inject mock seated/good hours for history that lacks the new fields. */
    suspend fun ensureWorkHourStatsSeeded() {
        WorkHourStatsMockSeeder.seedIfEmpty(
            workHourStatDao = workHourDao,
            existingEvents = dao.allOrdered(),
        )
    }

    suspend fun eventCount(): Int = dao.count()

    fun dashboardDays(
        stats: List<WorkHourStatEntity>,
        zone: ZoneId = ZoneId.systemDefault(),
        maxDays: Int = 7,
        nowMs: Long = System.currentTimeMillis(),
    ): List<DashboardDayStats> {
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val byDay = stats.groupBy {
            Instant.ofEpochMilli(it.hourStartMs).atZone(zone).toLocalDate()
        }.toMutableMap()

        // Always surface today (even with 0 hours) so an in-progress day is visible.
        if (!byDay.containsKey(today)) {
            byDay[today] = emptyList()
        }

        return byDay.entries
            .sortedByDescending { it.key }
            .take(maxDays)
            .map { (day, hours) ->
                val sorted = hours.sortedBy { it.hourStartMs }
                DashboardDayStats(
                    day = day,
                    hours = sorted,
                    seatedSeconds = sorted.sumOf { it.seatedSeconds },
                    goodPostureSeconds = sorted.sumOf { it.goodPostureSeconds },
                    inProgress = day == today,
                )
            }
    }

    private fun PostureEvent.toEntity() = PostureEventEntity(
        eventType = eventType.name,
        durationSeconds = durationSeconds,
        timestamp = timestamp,
    )
}

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
