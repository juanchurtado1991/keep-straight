package com.keepstraight.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.keepstraight.R
import com.keepstraight.data.model.DashboardDayStats
import com.keepstraight.data.local.WorkHourStatEntity
import com.keepstraight.ui.theme.PhoneDimens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.max

@Composable
fun WorkDayChartCard(
    day: DashboardDayStats,
    modifier: Modifier = Modifier,
) {
    val seatedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val goodColor = MaterialTheme.colorScheme.primary
    val labelFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PhoneDimens.itemGap),
    ) {
        Text(
            text = when {
                day.inProgress -> stringResource(R.string.dashboard_day_today_in_progress)
                else -> day.day.format(labelFormatter)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (day.seatedSeconds <= 0) {
            Text(
                text = stringResource(
                    if (day.inProgress) R.string.dashboard_day_empty_today
                    else R.string.dashboard_day_empty_past,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(
                    if (day.inProgress) R.string.dashboard_day_summary_so_far
                    else R.string.dashboard_day_summary,
                    formatDuration(day.seatedSeconds),
                    formatDuration(day.goodPostureSeconds),
                    (day.goodRatio * 100).toInt(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HourBarsChart(
                hours = day.hours,
                seatedColor = seatedColor,
                goodColor = goodColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PhoneDimens.Chart.height),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PhoneDimens.sectionGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendDot(color = seatedColor, label = stringResource(R.string.dashboard_legend_seated))
                LegendDot(color = goodColor, label = stringResource(R.string.dashboard_legend_good))
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PhoneDimens.Chart.legendGap),
    ) {
        Canvas(modifier = Modifier.size(PhoneDimens.Chart.legendDotSize)) {
            drawRoundRect(
                color = color,
                topLeft = Offset.Zero,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
        }
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun HourBarsChart(
    hours: List<WorkHourStatEntity>,
    seatedColor: Color,
    goodColor: Color,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val maxSeated = max(1, hours.maxOfOrNull { it.seatedSeconds } ?: 1)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (hours.isEmpty()) return@Canvas
            val barSlot = size.width / hours.size
            val barWidth = barSlot * 0.55f
            val chartBottom = size.height
            val chartHeight = chartBottom - 4.dp.toPx()

            hours.forEachIndexed { index, hour ->
                val x = barSlot * index + (barSlot - barWidth) / 2f
                val seatedH = (hour.seatedSeconds.toFloat() / maxSeated) * chartHeight
                val goodH = if (hour.seatedSeconds <= 0) {
                    0f
                } else {
                    (hour.goodPostureSeconds.toFloat() / hour.seatedSeconds) * seatedH
                }
                val seatedTop = chartBottom - seatedH
                val goodTop = chartBottom - goodH
                drawRoundRect(
                    color = seatedColor,
                    topLeft = Offset(x, seatedTop),
                    size = Size(barWidth, seatedH),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                )
                drawRoundRect(
                    color = goodColor,
                    topLeft = Offset(x, goodTop),
                    size = Size(barWidth, goodH),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            hours.forEach { hour ->
                val label = Instant.ofEpochMilli(hour.hourStartMs)
                    .atZone(zone)
                    .hour
                    .toString()
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                )
            }
        }
    }
}

fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}
