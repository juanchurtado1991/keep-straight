package com.keepstraight.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keepstraight.R
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.presentation.DiscoverError
import com.keepstraight.shared.presentation.DiscoverUiState
import com.keepstraight.shared.repository.PairedDevice
import com.keepstraight.ui.components.KeepStraightTopBar
import com.keepstraight.ui.theme.PhoneCard
import com.keepstraight.ui.theme.PhoneDimens
import com.keepstraight.ui.theme.phoneButtonShape
import com.keepstraight.ui.theme.phonePrimaryButtonColors
import com.keepstraight.ui.theme.phoneSecondaryButtonColors
import com.keepstraight.util.SystemIntentsHelper
import com.keepstraight.viewmodel.MainViewModel

const val STEP_WELCOME = 0
const val STEP_PAIR = 1
const val STEP_NOTIFICATIONS = 2
const val STEP_BATTERY = 3
const val STEP_WATCH_PERMISSIONS = 4
const val STEP_CALIBRATE = 5
const val STEP_SENSITIVITY = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onOpenNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenBatteryFallback: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenWearCompanion: (() -> Unit)?,
    onComplete: () -> Unit,
    initialStep: Int = STEP_WELCOME,
    pairOnly: Boolean = false,
) {
    var step by remember { mutableIntStateOf(initialStep) }
    var batteryAcknowledged by remember { mutableStateOf(false) }
    val discoverState by viewModel.discoverState.collectAsState()
    val availableNodes = (discoverState as? DiscoverUiState.Ready)?.nodes.orEmpty()
    val pairedWatchId by viewModel.pairedWatchId.collectAsState()
    val sensitivity by viewModel.sensitivity.collectAsState()
    val context = LocalContext.current
    val wearCompanionAvailable = remember {
        SystemIntentsHelper.isWearCompanionInstalled(context)
    }
    val discoverLoading = discoverState is DiscoverUiState.Loading
    val discoverFailed = discoverState as? DiscoverUiState.Failed

    LaunchedEffect(step) {
        if (step == STEP_PAIR) {
            viewModel.refreshWatchNodes()
        }
    }

    LaunchedEffect(Unit) {
        if (!viewModel.isBatteryOptimizationEnabled()) {
            batteryAcknowledged = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            KeepStraightTopBar(title = stringResource(R.string.app_name))
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = PhoneDimens.pagePadding)
                .padding(bottom = PhoneDimens.pagePadding),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = PhoneDimens.cardPadding),
                verticalArrangement = Arrangement.spacedBy(PhoneDimens.cardGap),
            ) {
                when (step) {
                    STEP_WELCOME -> OnboardingCopy(
                        title = stringResource(R.string.onboarding_welcome_title),
                        body = stringResource(R.string.onboarding_welcome_body),
                    )

                    STEP_PAIR -> {
                        OnboardingCopy(
                            title = stringResource(R.string.onboarding_pair_title),
                            body = stringResource(R.string.onboarding_pair_body),
                        )
                        PhoneCard {
                            OutlinedButton(
                                onClick = { viewModel.refreshWatchNodes() },
                                enabled = !discoverLoading,
                                modifier = Modifier.fillMaxWidth(),
                                shape = phoneButtonShape(),
                                colors = phoneSecondaryButtonColors(),
                            ) {
                                if (discoverLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(end = PhoneDimens.itemGap)
                                            .size(16.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                                Text(stringResource(R.string.onboarding_pair_refresh))
                            }
                            OutlinedButton(
                                onClick = onOpenBluetoothSettings,
                                modifier = Modifier.fillMaxWidth(),
                                shape = phoneButtonShape(),
                                colors = phoneSecondaryButtonColors(),
                            ) {
                                Text(stringResource(R.string.onboarding_pair_bluetooth))
                            }
                            if (wearCompanionAvailable && onOpenWearCompanion != null) {
                                OutlinedButton(
                                    onClick = onOpenWearCompanion,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = phoneButtonShape(),
                                    colors = phoneSecondaryButtonColors(),
                                ) {
                                    Text(stringResource(R.string.onboarding_pair_wear_companion))
                                }
                            }

                            when {
                                discoverLoading -> {
                                    Text(
                                        text = stringResource(R.string.onboarding_pair_searching),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                discoverFailed != null -> {
                                    Text(
                                        text = when (discoverFailed.reason) {
                                            DiscoverError.TIMEOUT ->
                                                stringResource(R.string.onboarding_pair_timeout)
                                            DiscoverError.FAILED ->
                                                stringResource(R.string.onboarding_pair_failed)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                                availableNodes.isEmpty() -> {
                                    Text(
                                        text = stringResource(R.string.onboarding_pair_none),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                else -> {
                                    if (availableNodes.size > 1) {
                                        Text(
                                            text = stringResource(R.string.onboarding_pair_multi),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    availableNodes.forEach { node ->
                                        WatchNodeRow(
                                            node = node,
                                            selected = node.id == pairedWatchId,
                                            onSelect = { viewModel.pairWatch(node.id) },
                                        )
                                    }
                                }
                            }
                            Text(
                                text = stringResource(R.string.onboarding_pair_optional),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    STEP_NOTIFICATIONS -> {
                        OnboardingCopy(
                            title = stringResource(R.string.onboarding_notifications_title),
                            body = stringResource(R.string.onboarding_notifications_body),
                        )
                        PhoneCard {
                            OutlinedButton(
                                onClick = onOpenNotificationSettings,
                                modifier = Modifier.fillMaxWidth(),
                                shape = phoneButtonShape(),
                                colors = phoneSecondaryButtonColors(),
                            ) {
                                Text(stringResource(R.string.onboarding_notifications_open))
                            }
                        }
                    }

                    STEP_BATTERY -> {
                        OnboardingCopy(
                            title = stringResource(R.string.onboarding_battery_title),
                            body = stringResource(R.string.onboarding_battery_body),
                        )
                        PhoneCard {
                            Button(
                                onClick = onOpenBatterySettings,
                                modifier = Modifier.fillMaxWidth(),
                                shape = phoneButtonShape(),
                                colors = phonePrimaryButtonColors(),
                            ) {
                                Text(stringResource(R.string.onboarding_battery_open))
                            }
                            OutlinedButton(
                                onClick = onOpenBatteryFallback,
                                modifier = Modifier.fillMaxWidth(),
                                shape = phoneButtonShape(),
                                colors = phoneSecondaryButtonColors(),
                            ) {
                                Text(stringResource(R.string.onboarding_battery_fallback))
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(PhoneDimens.itemGap),
                            ) {
                                Checkbox(
                                    checked = batteryAcknowledged,
                                    onCheckedChange = { batteryAcknowledged = it },
                                )
                                Text(
                                    text = stringResource(R.string.onboarding_battery_ack),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }

                    STEP_WATCH_PERMISSIONS -> OnboardingCopy(
                        title = stringResource(R.string.onboarding_watch_permissions_title),
                        body = stringResource(R.string.onboarding_watch_permissions_body),
                    )

                    STEP_CALIBRATE -> {
                        OnboardingCopy(
                            title = stringResource(R.string.onboarding_calibrate_title),
                            body = stringResource(R.string.onboarding_calibrate_body),
                        )
                        Text(
                            text = stringResource(R.string.onboarding_calibrate_required),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    STEP_SENSITIVITY -> {
                        OnboardingCopy(
                            title = stringResource(R.string.onboarding_sensitivity_title),
                            body = stringResource(R.string.onboarding_sensitivity_body),
                        )
                        PhoneCard {
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
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PhoneDimens.cardGap),
                horizontalArrangement = Arrangement.spacedBy(PhoneDimens.itemGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pairOnly) {
                    OutlinedButton(
                        onClick = onComplete,
                        shape = phoneButtonShape(),
                        colors = phoneSecondaryButtonColors(),
                    ) {
                        Text(stringResource(R.string.onboarding_back))
                    }
                } else if (step > STEP_WELCOME) {
                    OutlinedButton(
                        onClick = { step -= 1 },
                        shape = phoneButtonShape(),
                        colors = phoneSecondaryButtonColors(),
                    ) {
                        Text(stringResource(R.string.onboarding_back))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                val canProceed = when (step) {
                    STEP_PAIR -> pairedWatchId != null
                    STEP_BATTERY -> batteryAcknowledged || !viewModel.isBatteryOptimizationEnabled()
                    else -> true
                }
                if (step == STEP_PAIR && !pairOnly && pairedWatchId == null) {
                    OutlinedButton(
                        onClick = { step += 1 },
                        shape = phoneButtonShape(),
                        colors = phoneSecondaryButtonColors(),
                    ) {
                        Text(stringResource(R.string.onboarding_pair_skip))
                    }
                }
                Button(
                    onClick = {
                        when {
                            pairOnly && step == STEP_PAIR -> onComplete()
                            step == STEP_SENSITIVITY -> {
                                viewModel.completeOnboarding()
                                onComplete()
                            }
                            else -> step += 1
                        }
                    },
                    enabled = canProceed,
                    shape = phoneButtonShape(),
                    colors = phonePrimaryButtonColors(),
                ) {
                    Text(
                        when {
                            pairOnly && step == STEP_PAIR ->
                                stringResource(R.string.onboarding_pair_done)
                            step == STEP_SENSITIVITY ->
                                stringResource(R.string.onboarding_finish)
                            else -> stringResource(R.string.onboarding_next)
                        },
                    )
                }
            }
        }
    }
}

/** Title + body without a card — denser, more natural wizard copy on phone. */
@Composable
private fun OnboardingCopy(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(PhoneDimens.itemGap)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
            ),
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
        Text(
            text = node.displayName,
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
