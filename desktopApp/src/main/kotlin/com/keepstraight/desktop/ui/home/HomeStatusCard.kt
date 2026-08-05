package com.keepstraight.desktop.ui.home

import com.keepstraight.desktop.ui.i18n.DesktopStrings
import com.keepstraight.desktop.ui.i18n.StatusCopyResolver
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.keepstraight.desktop.ui.DesktopCard
import com.keepstraight.desktop.ui.desktopPrimaryButtonColors
import com.keepstraight.desktop.ui.desktopSecondaryButtonColors
import com.keepstraight.desktop.ui.theme.DesktopDimens
import com.keepstraight.desktop.ui.theme.StatusGood
import com.keepstraight.desktop.ui.theme.StatusGoodContainer
import com.keepstraight.desktop.ui.theme.StatusWarning
import com.keepstraight.desktop.ui.theme.StatusWarningContainer
import com.keepstraight.shared.presentation.DesktopStatusAction
import com.keepstraight.shared.presentation.DesktopStatusPresentation
import com.keepstraight.shared.presentation.DesktopStatusTone

@Composable
fun HomeStatusCard(
    status: DesktopStatusPresentation,
    slumpPercent: Int,
    onAction: (DesktopStatusAction) -> Unit,
) {
    val accent = toneAccent(status.tone)
    DesktopCard(containerColor = toneContainer(status.tone)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(DesktopDimens.StatusCard.dotSize)
                    .clip(CircleShape)
                    .background(accent),
            )
            Spacer(modifier = Modifier.width(DesktopDimens.rowGap))
            Text(
                StatusCopyResolver.presentationTitle(status),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (status.showProgress) {
                Spacer(modifier = Modifier.width(DesktopDimens.rowGap))
                CircularProgressIndicator(
                    modifier = Modifier.size(DesktopDimens.StatusCard.progressSize),
                    strokeWidth = DesktopDimens.StatusCard.progressStrokeWidth,
                    color = accent,
                )
            }
        }
        Text(StatusCopyResolver.presentationBody(status), style = MaterialTheme.typography.bodyLarge)
        StatusCopyResolver.presentationPresence(status)?.let {
            Text(
                DesktopStrings.homePresenceSlump(it, slumpPercent),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } ?: Text(
            DesktopStrings.homeSlumpScore(slumpPercent),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(DesktopDimens.rowGap)) {
            status.primaryAction?.let { action ->
                Button(
                    onClick = { onAction(action) },
                    colors = desktopPrimaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text(desktopActionLabel(action)) }
            }
            status.secondaryAction?.let { action ->
                OutlinedButton(
                    onClick = { onAction(action) },
                    colors = desktopSecondaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text(desktopActionLabel(action)) }
            }
        }
    }
}

@Composable
private fun toneAccent(tone: DesktopStatusTone): Color = when (tone) {
    DesktopStatusTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    DesktopStatusTone.PROGRESS -> MaterialTheme.colorScheme.primary
    DesktopStatusTone.SUCCESS -> StatusGood
    DesktopStatusTone.WARNING -> StatusWarning
    DesktopStatusTone.ERROR -> MaterialTheme.colorScheme.error
}

@Composable
private fun toneContainer(tone: DesktopStatusTone): Color = when (tone) {
    DesktopStatusTone.NEUTRAL -> MaterialTheme.colorScheme.surface
    DesktopStatusTone.PROGRESS -> MaterialTheme.colorScheme.primaryContainer
    DesktopStatusTone.SUCCESS -> StatusGoodContainer
    DesktopStatusTone.WARNING -> StatusWarningContainer
    DesktopStatusTone.ERROR -> MaterialTheme.colorScheme.errorContainer
}
