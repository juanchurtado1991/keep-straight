package com.keepstraight.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.keepstraight.R
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.ui.theme.PhoneDimens

@Composable
fun SensitivityRow(
    level: SensitivityLevel,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val label = when (level) {
        SensitivityLevel.STRICT -> stringResource(R.string.sensitivity_strict)
        SensitivityLevel.NORMAL -> stringResource(R.string.sensitivity_normal)
        SensitivityLevel.RELAXED -> stringResource(R.string.sensitivity_relaxed)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PhoneDimens.rowGap),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
            ),
        )
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
