package com.keepstraight.desktop.ui

import com.keepstraight.desktop.ui.i18n.DesktopStrings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    DesktopPage {
        Text(DesktopStrings.appName(), style = MaterialTheme.typography.headlineLarge)
        DesktopCard {
            Text(
                DesktopStrings.consentCameraBody(),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                DesktopStrings.consentOfflineBody(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(DesktopDimens.rowGap)) {
                Button(
                    onClick = onAccept,
                    colors = desktopPrimaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text(DesktopStrings.actionAccept()) }
                OutlinedButton(
                    onClick = onDecline,
                    colors = desktopSecondaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text(DesktopStrings.actionQuit()) }
            }
        }
    }
}
