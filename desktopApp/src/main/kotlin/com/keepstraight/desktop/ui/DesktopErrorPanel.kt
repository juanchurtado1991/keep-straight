package com.keepstraight.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

/**
 * Consistent info / error cards used by wizard, companion setup, and Home.
 */
@Composable
fun DesktopErrorPanel(
    title: String,
    body: String,
    detail: String? = null,
    primaryLabel: String? = "Retry",
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    DesktopCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        Text(body, style = MaterialTheme.typography.bodyLarge)
        detail?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PanelActions(primaryLabel, onPrimary, secondaryLabel, onSecondary)
    }
}

@Composable
fun DesktopInfoPanel(
    title: String,
    body: String,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    DesktopCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(body, style = MaterialTheme.typography.bodyLarge)
        PanelActions(primaryLabel, onPrimary, secondaryLabel, onSecondary)
    }
}

@Composable
private fun PanelActions(
    primaryLabel: String?,
    onPrimary: (() -> Unit)?,
    secondaryLabel: String?,
    onSecondary: (() -> Unit)?,
) {
    if ((primaryLabel == null || onPrimary == null) &&
        (secondaryLabel == null || onSecondary == null)
    ) {
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(DesktopDimens.rowGap)) {
        if (primaryLabel != null && onPrimary != null) {
            Button(
                onClick = onPrimary,
                colors = desktopPrimaryButtonColors(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(DesktopDimens.radiusSmall),
            ) { Text(primaryLabel) }
        }
        if (secondaryLabel != null && onSecondary != null) {
            OutlinedButton(
                onClick = onSecondary,
                colors = desktopSecondaryButtonColors(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(DesktopDimens.radiusSmall),
            ) { Text(secondaryLabel) }
        }
    }
}
