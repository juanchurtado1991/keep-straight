package com.keepstraight.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.keepstraight.R
import com.keepstraight.ui.theme.PhoneCard
import com.keepstraight.ui.theme.WarningContainer
import com.keepstraight.ui.theme.WarningOnContainer

@Composable
fun BatteryOptimizationBanner(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PhoneCard(
        modifier = modifier,
        containerColor = WarningContainer,
    ) {
        Text(
            text = stringResource(R.string.banner_battery_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = WarningOnContainer,
        )
        Text(
            text = stringResource(R.string.banner_battery_body),
            style = MaterialTheme.typography.bodyMedium,
            color = WarningOnContainer,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.banner_battery_dismiss), color = WarningOnContainer)
            }
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.banner_battery_fix), color = WarningOnContainer)
            }
        }
    }
}
