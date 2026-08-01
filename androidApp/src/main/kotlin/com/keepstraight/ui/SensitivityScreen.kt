package com.keepstraight.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.keepstraight.R
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.ui.components.KeepStraightTopBar
import com.keepstraight.viewmodel.MainViewModel

@Composable
fun SensitivityScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val sensitivity by viewModel.sensitivity.collectAsState()

    Scaffold(
        topBar = {
            KeepStraightTopBar(
                title = stringResource(R.string.sensitivity_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    SensitivityLevel.entries.forEach { level ->
                        val label = when (level) {
                            SensitivityLevel.STRICT -> stringResource(R.string.sensitivity_strict)
                            SensitivityLevel.NORMAL -> stringResource(R.string.sensitivity_normal)
                            SensitivityLevel.RELAXED -> stringResource(R.string.sensitivity_relaxed)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = sensitivity == level,
                                    onClick = { viewModel.setSensitivity(level) },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = sensitivity == level,
                                onClick = null,
                            )
                            Text(text = label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }
}
