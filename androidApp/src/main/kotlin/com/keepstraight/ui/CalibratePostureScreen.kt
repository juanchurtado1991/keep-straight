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
import com.keepstraight.shared.presentation.CalibrationError
import com.keepstraight.shared.presentation.CalibrationPhase
import com.keepstraight.shared.presentation.CalibrationUiState
import com.keepstraight.ui.components.KeepStraightTopBar
import com.keepstraight.ui.components.StatusPanel
import com.keepstraight.ui.components.StatusTone
import com.keepstraight.viewmodel.MainViewModel

@Composable
fun CalibratePostureScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenConnection: () -> Unit,
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val calibrationState by viewModel.calibrationState.collectAsState()

    LaunchedEffect(calibrationState) {
        if (calibrationState is CalibrationUiState.Success) {
            kotlinx.coroutines.delay(1_800)
            viewModel.resetCalibrationState()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            KeepStraightTopBar(
                title = stringResource(R.string.calibrate_title),
                onBack = {
                    viewModel.resetCalibrationState()
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
                        onPrimaryAction = viewModel::startCalibrationCountdown,
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
                    onPrimaryAction = viewModel::continueSlouchCalibration,
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
                        viewModel.resetCalibrationState()
                        viewModel.startCalibrationCountdown()
                    },
                    secondaryActionLabel = stringResource(R.string.connection_flow_title),
                    onSecondaryAction = onOpenConnection,
                    modifier = panelModifier,
                )
            }
        }
    }
}

@Composable
private fun calibrationErrorText(reason: CalibrationError): String = when (reason) {
    CalibrationError.NOT_CONNECTED -> stringResource(R.string.calibrate_error_not_connected)
    CalibrationError.SEND_FAILED -> stringResource(R.string.calibrate_error_send_failed)
    CalibrationError.SEND_TIMEOUT -> stringResource(R.string.calibrate_error_send_timeout)
    CalibrationError.WATCH_NO_RESPONSE -> stringResource(R.string.calibrate_error_no_response)
    CalibrationError.SAVE_FAILED -> stringResource(R.string.calibrate_error_save_failed)
    CalibrationError.SLUMP_TOO_SIMILAR -> stringResource(R.string.calibrate_error_slouch_similar)
}
