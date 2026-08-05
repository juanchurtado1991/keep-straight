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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.keepstraight.R
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.presentation.DiscoverError
import com.keepstraight.shared.presentation.DiscoverUiState
import com.keepstraight.ui.onboarding.OnboardingCopy
import com.keepstraight.ui.onboarding.OnboardingStep
import com.keepstraight.ui.onboarding.SensitivityRow
import com.keepstraight.ui.onboarding.WatchNodeRow
import com.keepstraight.ui.components.KeepStraightTopBar
import com.keepstraight.ui.theme.PhoneCard
import com.keepstraight.ui.theme.PhoneDimens
import com.keepstraight.ui.theme.phoneButtonShape
import com.keepstraight.ui.theme.phonePrimaryButtonColors
import com.keepstraight.ui.theme.phoneSecondaryButtonColors
import com.keepstraight.util.SystemIntentsHelper
import com.keepstraight.presentation.onboarding.OnboardingViewModel
import com.keepstraight.presentation.pairing.WatchPairingViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    pairingViewModel: WatchPairingViewModel,
    onboardingViewModel: OnboardingViewModel,
    onOpenNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenBatteryFallback: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenWearCompanion: (() -> Unit)?,
    onOpenCalibrate: () -> Unit,
    onComplete: () -> Unit,
    initialStep: OnboardingStep = OnboardingStep.WELCOME,
    pairOnly: Boolean = false,
) {
    var step by remember { mutableStateOf(initialStep) }
    var batteryAcknowledged by remember { mutableStateOf(false) }
    val discoverState by pairingViewModel.discoverState.collectAsState()
    val availableNodes = (discoverState as? DiscoverUiState.Ready)?.nodes.orEmpty()
    val pairedWatchId by pairingViewModel.pairedWatchId.collectAsState()
    val sensitivity by onboardingViewModel.sensitivity.collectAsState()
    val isCalibrated by onboardingViewModel.isCalibrated.collectAsState()
    val context = LocalContext.current
    val wearCompanionAvailable = remember {
        SystemIntentsHelper.isWearCompanionInstalled(context)
    }
    val discoverLoading = discoverState is DiscoverUiState.Loading
    val discoverFailed = discoverState as? DiscoverUiState.Failed

    LaunchedEffect(step) {
        if (step == OnboardingStep.PAIR) {
            pairingViewModel.refreshWatchNodes()
        }
    }

    LaunchedEffect(Unit) {
        if (!onboardingViewModel.isBatteryOptimizationEnabled()) {
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
                    OnboardingStep.WELCOME -> OnboardingCopy(
                        title = stringResource(R.string.onboarding_welcome_title),
                        body = stringResource(R.string.onboarding_welcome_body),
                    )

                    OnboardingStep.PAIR -> {
                        OnboardingCopy(
                            title = stringResource(R.string.onboarding_pair_title),
                            body = stringResource(R.string.onboarding_pair_body),
                        )
                        PhoneCard {
                            OutlinedButton(
                                onClick = { pairingViewModel.refreshWatchNodes() },
                                enabled = !discoverLoading,
                                modifier = Modifier.fillMaxWidth(),
                                shape = phoneButtonShape(),
                                colors = phoneSecondaryButtonColors(),
                            ) {
                                if (discoverLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(end = PhoneDimens.itemGap)
                                            .size(PhoneDimens.Onboarding.inlineProgressSize),
                                        strokeWidth = PhoneDimens.Onboarding.inlineProgressStrokeWidth,
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
                                            onSelect = { pairingViewModel.pairWatch(node.id) },
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

                    OnboardingStep.NOTIFICATIONS -> {
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

                    OnboardingStep.BATTERY -> {
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

                    OnboardingStep.WATCH_PERMISSIONS -> OnboardingCopy(
                        title = stringResource(R.string.onboarding_watch_permissions_title),
                        body = stringResource(R.string.onboarding_watch_permissions_body),
                    )

                    OnboardingStep.CALIBRATE -> {
                        OnboardingCopy(
                            title = stringResource(R.string.onboarding_calibrate_title),
                            body = stringResource(R.string.onboarding_calibrate_body),
                        )
                        Text(
                            text = stringResource(R.string.onboarding_calibrate_required),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = onOpenCalibrate,
                            shape = phoneButtonShape(),
                            colors = phonePrimaryButtonColors(),
                        ) {
                            Text(stringResource(R.string.onboarding_calibrate_again))
                        }
                    }

                    OnboardingStep.SENSITIVITY -> {
                        OnboardingCopy(
                            title = stringResource(R.string.onboarding_sensitivity_title),
                            body = stringResource(R.string.onboarding_sensitivity_body),
                        )
                        PhoneCard {
                            SensitivityLevel.entries.forEach { level ->
                                SensitivityRow(
                                    level = level,
                                    selected = sensitivity == level,
                                    onSelect = { onboardingViewModel.setSensitivity(level) },
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
                } else if (step.previous() != null) {
                    OutlinedButton(
                        onClick = { step = step.previous() ?: step },
                        shape = phoneButtonShape(),
                        colors = phoneSecondaryButtonColors(),
                    ) {
                        Text(stringResource(R.string.onboarding_back))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                val canProceed = when (step) {
                    OnboardingStep.PAIR -> pairedWatchId != null
                    OnboardingStep.BATTERY -> batteryAcknowledged || !onboardingViewModel.isBatteryOptimizationEnabled()
                    OnboardingStep.CALIBRATE -> isCalibrated
                    else -> true
                }
                if (step == OnboardingStep.PAIR && !pairOnly && pairedWatchId == null) {
                    OutlinedButton(
                        onClick = { step = step.next() ?: step },
                        shape = phoneButtonShape(),
                        colors = phoneSecondaryButtonColors(),
                    ) {
                        Text(stringResource(R.string.onboarding_pair_skip))
                    }
                }
                Button(
                    onClick = {
                        when {
                            pairOnly && step == OnboardingStep.PAIR -> onComplete()
                            step == OnboardingStep.SENSITIVITY -> {
                                onboardingViewModel.completeOnboarding()
                                onComplete()
                            }
                            else -> step = step.next() ?: step
                        }
                    },
                    enabled = canProceed,
                    shape = phoneButtonShape(),
                    colors = phonePrimaryButtonColors(),
                ) {
                    Text(
                        when {
                            pairOnly && step == OnboardingStep.PAIR ->
                                stringResource(R.string.onboarding_pair_done)
                            step == OnboardingStep.SENSITIVITY ->
                                stringResource(R.string.onboarding_finish)
                            else -> stringResource(R.string.onboarding_next)
                        },
                    )
                }
            }
        }
    }
}
