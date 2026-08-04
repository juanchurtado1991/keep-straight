package com.keepstraight.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.keepstraight.R
import com.keepstraight.ui.theme.PhoneCard
import com.keepstraight.ui.theme.PhoneDimens
import com.keepstraight.ui.theme.phoneButtonShape
import com.keepstraight.ui.theme.phonePrimaryButtonColors

/** Compact entry point — full reconnect UX lives on ConnectionFlowScreen. */
@Composable
fun ConnectionBanner(
    onOpenConnection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PhoneCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PhoneDimens.rowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PhoneDimens.itemGap / 2),
            ) {
                Text(
                    text = stringResource(R.string.banner_connection_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = stringResource(R.string.banner_connection_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Button(
                onClick = onOpenConnection,
                shape = phoneButtonShape(),
                colors = phonePrimaryButtonColors(),
            ) {
                Text(stringResource(R.string.dashboard_reconnect))
            }
        }
    }
}
