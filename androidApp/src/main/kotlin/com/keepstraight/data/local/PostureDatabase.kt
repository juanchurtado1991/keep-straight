package com.keepstraight.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PostureEventEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PostureDatabase : RoomDatabase() {

    abstract fun postureEventDao(): PostureEventDao

    companion object {
        private const val DATABASE_NAME = "keep_straight.db"

        fun create(context: Context): PostureDatabase =
            Room.databaseBuilder(context, PostureDatabase::class.java, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build()
    }
}
