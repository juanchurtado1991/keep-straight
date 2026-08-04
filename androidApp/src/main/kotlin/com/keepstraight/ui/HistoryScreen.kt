package com.keepstraight.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.keepstraight.R
import com.keepstraight.data.local.PostureEventEntity
import com.keepstraight.shared.model.PostureEventType
import com.keepstraight.ui.components.KeepStraightTopBar
import com.keepstraight.ui.theme.PhoneCard
import com.keepstraight.ui.theme.PhoneDimens
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
        containerColor = MaterialTheme.colorScheme.background,
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            events.loadState.refresh is LoadState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(PhoneDimens.pagePadding),
                ) {
                    PhoneCard {
                        Text(
                            text = stringResource(R.string.history_error),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            events.itemCount == 0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(PhoneDimens.pagePadding),
                ) {
                    PhoneCard {
                        Text(
                            text = stringResource(R.string.history_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(PhoneDimens.pagePadding),
                    verticalArrangement = Arrangement.spacedBy(PhoneDimens.sectionGap),
                ) {
                    items(
                        count = events.itemCount,
                        key = events.itemKey { it.id },
                    ) { index ->
                        val event = events[index] ?: return@items
                        val previous = if (index > 0) events[index - 1] else null
                        val currentDay = startOfDayMillis(event.timestamp)
                        val previousDay = previous?.let { startOfDayMillis(it.timestamp) }
                        val showHeader = currentDay != previousDay

                        Column(verticalArrangement = Arrangement.spacedBy(PhoneDimens.itemGap)) {
                            if (showHeader) {
                                Text(
                                    text = dayLabel(currentDay, event.timestamp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            PhoneCard {
                                EventRowContent(event)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRowContent(event: PostureEventEntity) {
    val timeFormat = SimpleDateFormat("HH:mm", EnglishLocale)
    val type = runCatching { PostureEventType.valueOf(event.eventType) }.getOrNull()
    val eventLabel = when (type) {
        PostureEventType.SLUMP_DETECTED -> stringResource(R.string.event_slump)
        PostureEventType.CALIBRATED -> stringResource(R.string.event_calibrated)
        PostureEventType.MONITORING_PAUSED -> stringResource(R.string.event_paused)
        PostureEventType.MONITORING_RESUMED -> stringResource(R.string.event_resumed)
        null -> event.eventType
    }
    val icon = eventIcon(type)
    val iconTint = when (type) {
        PostureEventType.SLUMP_DETECTED -> MaterialTheme.colorScheme.error
        PostureEventType.CALIBRATED -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val durationText = formatDuration(event.durationSeconds)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PhoneDimens.rowGap),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = eventLabel,
            tint = iconTint,
            modifier = Modifier.size(28.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PhoneDimens.itemGap / 2),
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
    }
}

private fun eventIcon(type: PostureEventType?): ImageVector = when (type) {
    PostureEventType.SLUMP_DETECTED -> Icons.Outlined.WarningAmber
    PostureEventType.CALIBRATED -> Icons.Outlined.CheckCircle
    PostureEventType.MONITORING_PAUSED -> Icons.Outlined.PauseCircle
    PostureEventType.MONITORING_RESUMED -> Icons.Outlined.PlayCircle
    null -> Icons.Outlined.CheckCircle
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
