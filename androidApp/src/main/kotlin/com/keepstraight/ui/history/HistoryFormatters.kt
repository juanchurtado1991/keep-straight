package com.keepstraight.ui.history

import java.util.Calendar

fun startOfDayMillis(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun formatDuration(seconds: Int): String {
    val minutes = seconds / HistoryDateConfig.SECONDS_PER_MINUTE
    val remaining = seconds % HistoryDateConfig.SECONDS_PER_MINUTE
    return if (minutes > 0) "${minutes}m ${remaining}s" else "${remaining}s"
}
