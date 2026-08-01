package com.keepstraight.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.keepstraight.R
import com.keepstraight.data.local.PostureEventEntity
import com.keepstraight.shared.model.PostureEventType
import com.keepstraight.ui.components.KeepStraightTopBar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val EnglishLocale = Locale.US

@Composable
fun HistoryScreen(
    events: LazyPagingItems<PostureEventEntity>,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            KeepStraightTopBar(
                title = stringResource(R.string.history_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        when {
            events.loadState.refresh is LoadState.Loading && events.itemCount == 0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            events.loadState.refresh is LoadState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                ) {
                    Text(
                        text = stringResource(R.string.history_error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            events.itemCount == 0 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                ) {
                    Text(
                        text = stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    items(
                        count = events.itemCount,
                        key = events.itemKey { it.id },
                    ) { index ->
                        val event = events[index] ?: return@items
                        val previous = if (index > 0) events[index - 1] else null
                        val currentDay = startOfDayMillis(event.timestamp)
                        val previousDay = previous?.let { startOfDayMillis(it.timestamp) }

                        if (currentDay != previousDay) {
                            DayHeader(dayLabel(currentDay, event.timestamp))
                        }
                        EventRow(event)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.primary,
    )
    HorizontalDivider()
}

@Composable
private fun EventRow(event: PostureEventEntity) {
    val timeFormat = SimpleDateFormat("HH:mm", EnglishLocale)
    val type = runCatching { PostureEventType.valueOf(event.eventType) }.getOrNull()
    val eventLabel = when (type) {
        PostureEventType.SLUMP_DETECTED -> stringResource(R.string.event_slump)
        PostureEventType.CALIBRATED -> stringResource(R.string.event_calibrated)
        PostureEventType.MONITORING_PAUSED -> stringResource(R.string.event_paused)
        PostureEventType.MONITORING_RESUMED -> stringResource(R.string.event_resumed)
        null -> event.eventType
    }
    val durationText = formatDuration(event.durationSeconds)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text = eventLabel, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = timeFormat.format(Date(event.timestamp)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (type == PostureEventType.SLUMP_DETECTED && event.durationSeconds > 0) {
            Text(
                text = stringResource(R.string.event_duration, durationText),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    HorizontalDivider()
}

@Composable
private fun dayLabel(startOfDay: Long, timestamp: Long): String {
    val todayStart = startOfDayMillis(System.currentTimeMillis())
    val yesterdayCal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }
    val yesterdayStart = startOfDayMillis(yesterdayCal.timeInMillis)

    return when (startOfDay) {
        todayStart -> stringResource(R.string.history_today)
        yesterdayStart -> stringResource(R.string.history_yesterday)
        else -> SimpleDateFormat("EEEE, MMM d", EnglishLocale).format(Date(timestamp))
    }
}

private fun startOfDayMillis(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remaining = seconds % 60
    return if (minutes > 0) "${minutes}m ${remaining}s" else "${remaining}s"
}
