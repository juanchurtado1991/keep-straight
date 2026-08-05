package com.keepstraight.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.WatchOff
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.keepstraight.R
import com.keepstraight.presentation.calibration.CalibrationUiConfig
import com.keepstraight.presentation.calibration.CalibrationViewModel
import com.keepstraight.shared.presentation.CalibrationError
import com.keepstraight.shared.presentation.CalibrationPhase
import com.keepstraight.shared.presentation.CalibrationUiState
import com.keepstraight.shared.presentation.phone.CalibrationEffect
import com.keepstraight.shared.presentation.phone.CalibrationEvent
import com.keepstraight.ui.components.KeepStraightTopBar
import com.keepstraight.ui.components.StatusPanel
import com.keepstraight.ui.components.StatusTone
import com.keepstraight.ui.calibration.calibrationErrorText

@Composable
fun CalibratePostureScreen(
    viewModel: CalibrationViewModel,
    onBack: () -> Unit,
    onOpenConnection: () -> Unit,
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val calibrationState by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CalibrationEffect.NavigateBack -> onBack()
            }
        }
    }

    LaunchedEffect(calibrationState) {
        if (calibrationState is CalibrationUiState.Success) {
            kotlinx.coroutines.delay(CalibrationUiConfig.SUCCESS_DISMISS_MS)
            viewModel.onEvent(CalibrationEvent.Reset)
            viewModel.onEvent(CalibrationEvent.SuccessAcknowledged)
        }
    }

    Scaffold(
        topBar = {
            KeepStraightTopBar(
                title = stringResource(R.string.calibrate_title),
                onBack = {
                    viewModel.onEvent(CalibrationEvent.Reset)
                    onBack()
                },
            )
        },
    ) { padding ->
        val panelModifier = Modifier.padding(padding)
        when (val state = calibrationState) {
            CalibrationUiState.Idle -> {
                if (!isConnected) {
                    StatusPanel(
                        tone = StatusTone.WARNING,
                        icon = Icons.Outlined.WatchOff,
                        title = stringResource(R.string.calibrate_offline_title),
                        body = stringResource(R.string.calibrate_error_not_connected),
                        primaryActionLabel = stringResource(R.string.connection_flow_title),
                        onPrimaryAction = onOpenConnection,
                        secondaryActionLabel = stringResource(R.string.action_back),
                        onSecondaryAction = onBack,
                        modifier = panelModifier,
                    )
                } else {
                    StatusPanel(
                        tone = StatusTone.NEUTRAL,
                        icon = Icons.Outlined.SelfImprovement,
                        title = stringResource(R.string.calibrate_ready_title),
                        body = stringResource(R.string.calibrate_instructions),
                        primaryActionLabel = stringResource(R.string.calibrate_start),
                        onPrimaryAction = { viewModel.onEvent(CalibrationEvent.Start) },
                        modifier = panelModifier,
                    )
                }
            }

            CalibrationUiState.PromptSlouch -> {
                StatusPanel(
                    tone = StatusTone.NEUTRAL,
                    icon = Icons.Outlined.SelfImprovement,
                    title = stringResource(R.string.calibrate_slouch_title),
                    body = stringResource(R.string.calibrate_slouch_body),
                    primaryActionLabel = stringResource(R.string.calibrate_slouch_capture),
                    onPrimaryAction = { viewModel.onEvent(CalibrationEvent.ContinueSlouch) },
                    modifier = panelModifier,
                )
            }

            is CalibrationUiState.Countdown -> {
                val body = when (state.phase) {
                    CalibrationPhase.GOOD -> stringResource(R.string.calibrate_countdown_body)
                    CalibrationPhase.SLOUCH -> stringResource(R.string.calibrate_countdown_slouch_body)
                }
                StatusPanel(
                    tone = StatusTone.PROGRESS,
                    title = state.seconds.toString(),
                    body = body,
                    modifier = panelModifier,
                )
            }

            is CalibrationUiState.Capturing -> {
                val body = when (state.phase) {
                    CalibrationPhase.GOOD -> stringResource(R.string.calibrate_capturing_hint)
                    CalibrationPhase.SLOUCH -> stringResource(R.string.calibrate_capturing_slouch_hint)
                }
                StatusPanel(
                    tone = StatusTone.PROGRESS,
                    showProgress = true,
                    title = stringResource(R.string.calibrate_capturing),
                    body = body,
                    modifier = panelModifier,
                )
            }

            CalibrationUiState.Success -> {
                StatusPanel(
                    tone = StatusTone.SUCCESS,
                    icon = Icons.Outlined.CheckCircle,
                    title = stringResource(R.string.calibrate_success_title),
                    body = stringResource(R.string.calibrate_success_body),
                    modifier = panelModifier,
                )
            }

            is CalibrationUiState.Error -> {
                StatusPanel(
                    tone = StatusTone.ERROR,
                    icon = Icons.Outlined.ErrorOutline,
                    title = stringResource(R.string.calibrate_failed_title),
                    body = calibrationErrorText(state.reason),
                    primaryActionLabel = stringResource(R.string.calibrate_retry),
                    onPrimaryAction = {
                        viewModel.onEvent(CalibrationEvent.Reset)
                        viewModel.onEvent(CalibrationEvent.Start)
                    },
                    secondaryActionLabel = stringResource(R.string.connection_flow_title),
                    onSecondaryAction = onOpenConnection,
                    modifier = panelModifier,
                )
            }
        }
    }
}
