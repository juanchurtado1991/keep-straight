package com.keepstraight.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.keepstraight.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun historyDayLabel(startOfDay: Long, timestamp: Long): String {
    val todayStart = startOfDayMillis(System.currentTimeMillis())
    val yesterdayCal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }
    val yesterdayStart = startOfDayMillis(yesterdayCal.timeInMillis)

    return when (startOfDay) {
        todayStart -> stringResource(R.string.history_today)
        yesterdayStart -> stringResource(R.string.history_yesterday)
        else -> SimpleDateFormat(
            HistoryDateConfig.DAY_PATTERN,
            HistoryDateConfig.displayLocale,
        ).format(Date(timestamp))
    }
}
