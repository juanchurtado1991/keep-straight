package com.keepstraight.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.keepstraight.R
import com.keepstraight.shared.repository.PairedDevice
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.viewmodel.MainViewModel

private const val STEP_WELCOME = 0
private const val STEP_PAIR = 1
private const val STEP_NOTIFICATIONS = 2
private const val STEP_BATTERY = 3
private const val STEP_CALIBRATE = 4
private const val STEP_SENSITIVITY = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onOpenNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onCalibrate: () -> Unit,
    onComplete: () -> Unit,
) {
    var step by remember { mutableIntStateOf(STEP_WELCOME) }
    var batteryAcknowledged by remember { mutableStateOf(false) }
    val availableNodes by viewModel.availableNodes.collectAsState()
    val pairedWatchId by viewModel.pairedWatchId.collectAsState()
    val sensitivity by viewModel.sensitivity.collectAsState()

    LaunchedEffect(step) {
        if (step == STEP_PAIR) {
            viewModel.refreshWatchNodes()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                when (step) {
                    STEP_WELCOME -> OnboardingStep(
                        title = stringResource(R.string.onboarding_welcome_title),
                        body = stringResource(R.string.onboarding_welcome_body),
                    )

                    STEP_PAIR -> {
                        OnboardingStep(
                            title = stringResource(R.string.onboarding_pair_title),
                            body = stringResource(R.string.onboarding_pair_body),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = {
                            viewModel.refreshWatchNodes()
                            onOpenBluetoothSettings()
                        }) {
                            Text(stringResource(R.string.onboarding_pair_refresh))
                        }
                        if (availableNodes.isEmpty()) {
                            Text(
                                text = stringResource(R.string.onboarding_pair_none),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        } else {
                            availableNodes.forEach { node ->
                                WatchNodeRow(
                                    node = node,
                                    selected = node.id == pairedWatchId,
                                    onSelect = { viewModel.pairWatch(node.id) },
                                )
                            }
                        }
                    }

                    STEP_NOTIFICATIONS -> {
                        OnboardingStep(
                            title = stringResource(R.string.onboarding_notifications_title),
                            body = stringResource(R.string.onboarding_notifications_body),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = onOpenNotificationSettings) {
                            Text(stringResource(R.string.onboarding_notifications_open))
                        }
                    }

                    STEP_BATTERY -> {
                        OnboardingStep(
                            title = stringResource(R.string.onboarding_battery_title),
                            body = stringResource(R.string.onboarding_battery_body),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = onOpenBatterySettings) {
                            Text(stringResource(R.string.onboarding_battery_open))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 12.dp),
                        ) {
                            Checkbox(
                                checked = batteryAcknowledged,
                                onCheckedChange = { batteryAcknowledged = it },
                            )
                            Text(stringResource(R.string.onboarding_battery_ack))
                        }
                    }

                    STEP_CALIBRATE -> {
                        OnboardingStep(
                            title = stringResource(R.string.onboarding_calibrate_title),
                            body = stringResource(R.string.onboarding_calibrate_body),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onCalibrate, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.calibrate_start))
                        }
                    }

                    STEP_SENSITIVITY -> {
                        OnboardingStep(
                            title = stringResource(R.string.onboarding_sensitivity_title),
                            body = stringResource(R.string.onboarding_sensitivity_body),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SensitivityLevel.entries.forEach { level ->
                            SensitivityRow(
                                level = level,
                                selected = sensitivity == level,
                                onSelect = { viewModel.setSensitivity(level) },
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (step > STEP_WELCOME) {
                    OutlinedButton(onClick = { step -= 1 }) {
                        Text(stringResource(R.string.onboarding_back))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                val canProceed = when (step) {
                    STEP_PAIR -> pairedWatchId != null
                    STEP_BATTERY -> batteryAcknowledged || !viewModel.isBatteryOptimizationEnabled()
                    else -> true
                }
                Button(
                    onClick = {
                        if (step == STEP_SENSITIVITY) {
                            viewModel.completeOnboarding()
                            onComplete()
                        } else {
                            step += 1
                        }
                    },
                    enabled = canProceed,
                ) {
                    Text(
                        if (step == STEP_SENSITIVITY) {
                            stringResource(R.string.onboarding_finish)
                        } else {
                            stringResource(R.string.onboarding_next)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingStep(title: String, body: String) {
    Text(text = title, style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(12.dp))
    Text(text = body, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun WatchNodeRow(
    node: PairedDevice,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = node.displayName,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SensitivityRow(
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
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}
