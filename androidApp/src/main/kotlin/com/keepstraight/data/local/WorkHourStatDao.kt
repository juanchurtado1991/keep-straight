package com.keepstraight.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkHourStatDao {

    @Query(
        """
        SELECT * FROM work_hour_stats
        WHERE hourStartMs >= :fromMs AND hourStartMs < :toMs
        ORDER BY hourStartMs ASC
        """,
    )
    fun observeRange(fromMs: Long, toMs: Long): Flow<List<WorkHourStatEntity>>

    @Query(
        """
        SELECT * FROM work_hour_stats
        WHERE hourStartMs >= :fromMs
        ORDER BY hourStartMs ASC
        """,
    )
    fun observeFrom(fromMs: Long): Flow<List<WorkHourStatEntity>>

    @Query("SELECT COUNT(*) FROM work_hour_stats")
    suspend fun count(): Int

    @Query("SELECT * FROM work_hour_stats WHERE hourStartMs = :hourStartMs LIMIT 1")
    suspend fun get(hourStartMs: Long): WorkHourStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: WorkHourStatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(stats: List<WorkHourStatEntity>)

    @Transaction
    suspend fun addDelta(hourStartMs: Long, seatedDeltaSec: Int, goodPostureDeltaSec: Int) {
        if (seatedDeltaSec <= 0 && goodPostureDeltaSec <= 0) return
        val existing = get(hourStartMs)
        upsert(
            WorkHourStatEntity(
                hourStartMs = hourStartMs,
                seatedSeconds = (existing?.seatedSeconds ?: 0) + seatedDeltaSec.coerceAtLeast(0),
                goodPostureSeconds = (existing?.goodPostureSeconds ?: 0) +
                    goodPostureDeltaSec.coerceAtLeast(0),
            ),
        )
    }
}
