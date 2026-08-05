package com.keepstraight.desktop.ui.wizard

import com.keepstraight.desktop.ui.i18n.DesktopStrings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.keepstraight.desktop.DesktopSessionController
import com.keepstraight.desktop.ui.ConsentDialog
import com.keepstraight.desktop.ui.DesktopCard
import com.keepstraight.desktop.ui.DesktopDimens
import com.keepstraight.desktop.ui.DesktopPage
import com.keepstraight.desktop.ui.desktopPrimaryButtonColors
import com.keepstraight.desktop.ui.desktopSecondaryButtonColors

private enum class WizardPhase {
    Welcome,
    Camera,
    Companion,
}

@Composable
fun FirstRunWizard(
    controller: DesktopSessionController,
    prefsAcceptedCamera: Boolean,
    onAcceptCamera: () -> Unit,
    onDeclineCamera: () -> Unit,
    onFinished: () -> Unit,
) {
    var phase by remember { mutableStateOf(WizardPhase.Welcome) }
    var cameraDone by remember { mutableStateOf(prefsAcceptedCamera) }
    var skipCompanion by remember { mutableStateOf(false) }

    when (phase) {
        WizardPhase.Welcome -> DesktopPage {
            Text(DesktopStrings.wizardWelcomeTitle(), style = MaterialTheme.typography.headlineLarge)
            DesktopCard {
                Text(
                    DesktopStrings.wizardWelcomeBody(),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    DesktopStrings.wizardWelcomeCompanion(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DesktopPreflight.hints().forEach { hint ->
                    Text(
                        hint.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        modifier = Modifier.padding(top = DesktopDimens.rowGap),
                    )
                    Text(
                        hint.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(DesktopDimens.rowGap)) {
                    Button(
                        onClick = {
                            skipCompanion = false
                            phase = if (cameraDone) WizardPhase.Companion else WizardPhase.Camera
                        },
                        colors = desktopPrimaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) { Text(DesktopStrings.actionContinue()) }
                    OutlinedButton(
                        onClick = {
                            skipCompanion = true
                            if (cameraDone) {
                                onFinished()
                            } else {
                                phase = WizardPhase.Camera
                            }
                        },
                        colors = desktopSecondaryButtonColors(),
                        shape = RoundedCornerShape(DesktopDimens.radiusSmall),
                    ) { Text(DesktopStrings.actionSkipPhoneWatch()) }
                }
            }
        }

        WizardPhase.Camera -> ConsentDialog(
            onAccept = {
                onAcceptCamera()
                cameraDone = true
                if (skipCompanion) {
                    onFinished()
                } else {
                    phase = WizardPhase.Companion
                }
            },
            onDecline = onDeclineCamera,
        )

        WizardPhase.Companion -> CompanionSetupFlow(
            controller = controller,
            onFinished = onFinished,
        )
    }
}
