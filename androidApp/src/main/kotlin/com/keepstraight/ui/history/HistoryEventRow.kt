package com.keepstraight.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.keepstraight.R
import com.keepstraight.data.local.PostureEventEntity
import com.keepstraight.shared.model.PostureEventType
import com.keepstraight.ui.theme.PhoneDimens
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun HistoryEventRow(event: PostureEventEntity) {
    val timeFormat = SimpleDateFormat(HistoryDateConfig.TIME_PATTERN, HistoryDateConfig.englishLocale)
    val type = runCatching { PostureEventType.valueOf(event.eventType) }.getOrNull()
    val eventLabel = when (type) {
        PostureEventType.SLUMP_DETECTED -> stringResource(R.string.event_slump)
        PostureEventType.CALIBRATED -> stringResource(R.string.event_calibrated)
        PostureEventType.MONITORING_PAUSED -> stringResource(R.string.event_paused)
        PostureEventType.MONITORING_RESUMED -> stringResource(R.string.event_resumed)
        null -> event.eventType
    }
    val icon = historyEventIcon(type)
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
            modifier = Modifier.size(PhoneDimens.History.eventIconSize),
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

private fun historyEventIcon(type: PostureEventType?): ImageVector = when (type) {
    PostureEventType.SLUMP_DETECTED -> Icons.Outlined.WarningAmber
    PostureEventType.CALIBRATED -> Icons.Outlined.CheckCircle
    PostureEventType.MONITORING_PAUSED -> Icons.Outlined.PauseCircle
    PostureEventType.MONITORING_RESUMED -> Icons.Outlined.PlayCircle
    null -> Icons.Outlined.CheckCircle
}
