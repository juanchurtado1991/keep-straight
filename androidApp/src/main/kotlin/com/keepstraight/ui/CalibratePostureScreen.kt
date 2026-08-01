package com.keepstraight.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.keepstraight.R
import com.keepstraight.ui.components.KeepStraightTopBar
import com.keepstraight.shared.presentation.CalibrationUiState
import com.keepstraight.viewmodel.MainViewModel

@Composable
fun CalibratePostureScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val calibrationState by viewModel.calibrationState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.calibrationResult.collect { result ->
            viewModel.onCalibrationResult(result)
        }
    }

    LaunchedEffect(calibrationState) {
        if (calibrationState is CalibrationUiState.Success) {
            kotlinx.coroutines.delay(1_500)
            viewModel.resetCalibrationState()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            KeepStraightTopBar(
                title = stringResource(R.string.calibrate_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.calibrate_instructions),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            when (val state = calibrationState) {
                CalibrationUiState.Idle -> {
                    if (!isConnected) {
                        Text(
                            text = stringResource(R.string.calibrate_not_connected),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    } else {
                        Button(
                            onClick = viewModel::startCalibrationCountdown,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.calibrate_start))
                        }
                    }
                }

                is CalibrationUiState.Countdown -> {
                    Text(
                        text = stringResource(R.string.calibrate_countdown, state.seconds),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                CalibrationUiState.Capturing -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.calibrate_capturing),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                CalibrationUiState.Success -> {
                    Text(
                        text = stringResource(R.string.calibrate_success),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                CalibrationUiState.Error -> {
                    Text(
                        text = stringResource(R.string.calibrate_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = viewModel::resetCalibrationState) {
                        Text(stringResource(R.string.calibrate_start))
                    }
                }
            }
        }
    }
}
