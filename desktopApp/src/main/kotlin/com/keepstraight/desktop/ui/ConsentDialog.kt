package com.keepstraight.desktop.ui

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
        Text("KeepStraight", style = MaterialTheme.typography.headlineLarge)
        DesktopCard {
            Text(
                "Camera is used only for live posture. Frames are not saved.",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Pose runs 100% offline on this computer. A privacy LED may stay on while the camera is open.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(DesktopDimens.rowGap)) {
                Button(
                    onClick = onAccept,
                    colors = desktopPrimaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text("Accept") }
                OutlinedButton(
                    onClick = onDecline,
                    colors = desktopSecondaryButtonColors(),
                    shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                ) { Text("Quit") }
            }
        }
    }
}
