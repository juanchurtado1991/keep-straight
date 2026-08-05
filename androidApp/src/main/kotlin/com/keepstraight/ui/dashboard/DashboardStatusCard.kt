package com.keepstraight.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.keepstraight.ui.theme.PhoneCard

@Composable
fun DashboardStatusCard(
    icon: ImageVector,
    title: String,
    body: String,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PhoneCard(
        modifier = modifier.clickable(onClick = onClick),
        containerColor = containerColor,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
