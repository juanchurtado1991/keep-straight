package com.keepstraight.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.paging.PagingSource

@Dao
interface PostureEventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: PostureEventEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(events: List<PostureEventEntity>)

    @Query("SELECT * FROM posture_events ORDER BY timestamp DESC")
    fun pagingSource(): PagingSource<Int, PostureEventEntity>

    @Query("SELECT COUNT(*) FROM posture_events")
    suspend fun count(): Int

    @Query("SELECT * FROM posture_events ORDER BY timestamp ASC")
    suspend fun allOrdered(): List<PostureEventEntity>
}
