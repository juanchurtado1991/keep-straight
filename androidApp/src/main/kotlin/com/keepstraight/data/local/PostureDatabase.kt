package com.keepstraight.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PostureEventEntity::class, WorkHourStatEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class PostureDatabase : RoomDatabase() {

    abstract fun postureEventDao(): PostureEventDao
    abstract fun workHourStatDao(): WorkHourStatDao

    companion object {
        private const val DATABASE_NAME = "keep_straight.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS work_hour_stats (
                        hourStartMs INTEGER PRIMARY KEY NOT NULL,
                        seatedSeconds INTEGER NOT NULL,
                        goodPostureSeconds INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_work_hour_stats_hourStartMs " +
                        "ON work_hour_stats(hourStartMs)",
                )
            }
        }

        fun create(context: Context): PostureDatabase =
            Room.databaseBuilder(context, PostureDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
