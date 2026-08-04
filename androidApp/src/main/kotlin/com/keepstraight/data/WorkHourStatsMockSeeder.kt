package com.keepstraight.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Random
import com.keepstraight.data.local.PostureEventEntity
import com.keepstraight.data.local.WorkHourStatEntity
import com.keepstraight.data.local.WorkHourStatDao

/**
 * Backfills [WorkHourStatEntity] for history that predates seated/good-posture tracking.
 * Stable per-day mocks so charts look consistent across launches.
 * Today is truncated to the current hour so an in-progress day still shows partial bars.
 */
object WorkHourStatsMockSeeder {

    suspend fun seedIfEmpty(
        workHourStatDao: WorkHourStatDao,
        existingEvents: List<PostureEventEntity>,
        zone: ZoneId = ZoneId.systemDefault(),
        nowMs: Long = System.currentTimeMillis(),
    ) {
        if (workHourStatDao.count() > 0) return
        // Only history that predates the new fields gets backfilled. A fresh install must show
        // the real empty state instead of invented desk time.
        if (existingEvents.isEmpty()) return

        val days = daysToSeed(existingEvents, zone, nowMs)
        val stats = mutableListOf<WorkHourStatEntity>()
        for (day in days) {
            stats += mockDay(day, existingEvents, zone, nowMs)
        }
        if (stats.isNotEmpty()) {
            workHourStatDao.upsertAll(stats)
        }
    }

    private fun daysToSeed(
        events: List<PostureEventEntity>,
        zone: ZoneId,
        nowMs: Long,
    ): List<LocalDate> {
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val fromEvents = events
            .map { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
            .distinct()
            .sorted()
        // Always include today so the dashboard has an in-progress card.
        val base = fromEvents.filter { it <= today }.takeLast(14).toMutableList()
        if (!base.contains(today)) base.add(today)
        return base.distinct().sorted()
    }

    private fun mockDay(
        day: LocalDate,
        events: List<PostureEventEntity>,
        zone: ZoneId,
        nowMs: Long,
    ): List<WorkHourStatEntity> {
        val rng = Random(day.toEpochDay())
        val now = Instant.ofEpochMilli(nowMs).atZone(zone)
        val today = now.toLocalDate()
        val currentHour = now.hour
        val minuteFraction = (now.minute + now.second / 60.0) / 60.0

        val dayEvents = events.filter {
            Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() == day
        }
        val workHours = 9..17
        return workHours.mapNotNull { hour ->
            // In-progress day: no future hours; current hour is partial.
            if (day == today && hour > currentHour) return@mapNotNull null
            if (day == today && hour == currentHour && minuteFraction < 0.02) {
                return@mapNotNull null
            }

            val hourStart = day.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
            val hourEnd = day.atTime(hour, 0).plusHours(1).atZone(zone).toInstant().toEpochMilli()
            val slumpsInHour = dayEvents.count { it.timestamp in hourStart until hourEnd }

            var seated = 25 * 60 + rng.nextInt(28 * 60)
            var good = (seated * (0.62 + rng.nextDouble() * 0.28)).toInt()
            good = (good - slumpsInHour * (4 * 60 + rng.nextInt(3 * 60))).coerceAtLeast(seated / 5)
            good = good.coerceAtMost(seated)

            if (dayEvents.isEmpty() && (day.dayOfWeek.value >= 6 || hour !in 10..16)) {
                seated = (seated * 0.35).toInt()
                good = (good * 0.35).toInt()
            }

            if (day == today && hour == currentHour) {
                val scale = minuteFraction.coerceIn(0.05, 1.0)
                seated = (seated * scale).toInt()
                good = (good * scale).toInt().coerceAtMost(seated)
            }

            if (seated <= 60) return@mapNotNull null
            WorkHourStatEntity(
                hourStartMs = hourStart,
                seatedSeconds = seated,
                goodPostureSeconds = good,
            )
        }
    }

    fun hourBucketStart(timestampMs: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val zdt = Instant.ofEpochMilli(timestampMs).atZone(zone)
        return zdt.truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli()
    }
}
