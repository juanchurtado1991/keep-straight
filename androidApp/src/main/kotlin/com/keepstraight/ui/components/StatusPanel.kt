package com.keepstraight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.keepstraight.ui.theme.PhoneDimens
import com.keepstraight.ui.theme.StatusGood
import com.keepstraight.ui.theme.StatusGoodContainer
import com.keepstraight.ui.theme.WarningContainer
import com.keepstraight.ui.theme.WarningOnContainer
import com.keepstraight.ui.theme.phoneButtonShape
import com.keepstraight.ui.theme.phonePrimaryButtonColors
import com.keepstraight.ui.theme.phoneSecondaryButtonColors

@Composable
fun StatusPanel(
    tone: StatusTone,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    showProgress: Boolean = false,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    primaryEnabled: Boolean = true,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    secondaryEnabled: Boolean = true,
) {
    val accent = toneColor(tone)
    val container = toneContainer(tone)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(PhoneDimens.pagePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PhoneDimens.sectionGap, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(PhoneDimens.StatusPanel.iconCircleSize)
                .clip(CircleShape)
                .background(container),
            contentAlignment = Alignment.Center,
        ) {
            when {
                showProgress -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(PhoneDimens.StatusPanel.progressIndicatorSize),
                        color = accent,
                        strokeWidth = PhoneDimens.StatusPanel.progressStrokeWidth,
                    )
                }
                icon != null -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(PhoneDimens.StatusPanel.iconSize),
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .size(PhoneDimens.StatusPanel.dotSize)
                            .clip(CircleShape)
                            .background(accent),
                    )
                }
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        if (primaryActionLabel != null && onPrimaryAction != null) {
            Button(
                onClick = onPrimaryAction,
                enabled = primaryEnabled,
                modifier = Modifier.fillMaxWidth(),
                shape = phoneButtonShape(),
                colors = phonePrimaryButtonColors(),
            ) {
                if (showProgress && !primaryEnabled) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = PhoneDimens.itemGap)
                            .size(PhoneDimens.StatusPanel.dotSize),
                        strokeWidth = PhoneDimens.StatusPanel.inlineProgressStrokeWidth,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(primaryActionLabel)
            }
        }

        if (secondaryActionLabel != null && onSecondaryAction != null) {
            OutlinedButton(
                onClick = onSecondaryAction,
                enabled = secondaryEnabled,
                modifier = Modifier.fillMaxWidth(),
                shape = phoneButtonShape(),
                colors = phoneSecondaryButtonColors(),
            ) {
                Text(secondaryActionLabel)
            }
        }
    }
}

@Composable
private fun toneColor(tone: StatusTone): Color = when (tone) {
    StatusTone.NEUTRAL, StatusTone.PROGRESS -> MaterialTheme.colorScheme.primary
    StatusTone.SUCCESS -> StatusGood
    StatusTone.WARNING -> WarningOnContainer
    StatusTone.ERROR -> MaterialTheme.colorScheme.error
}

@Composable
private fun toneContainer(tone: StatusTone): Color = when (tone) {
    StatusTone.NEUTRAL, StatusTone.PROGRESS -> MaterialTheme.colorScheme.primaryContainer
    StatusTone.SUCCESS -> StatusGoodContainer
    StatusTone.WARNING -> WarningContainer
    StatusTone.ERROR -> MaterialTheme.colorScheme.errorContainer
}
