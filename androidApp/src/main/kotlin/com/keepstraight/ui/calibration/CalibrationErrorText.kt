package com.keepstraight.ui.calibration

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.keepstraight.R
import com.keepstraight.shared.presentation.CalibrationError

@Composable
fun calibrationErrorText(reason: CalibrationError): String = when (reason) {
    CalibrationError.NOT_CONNECTED -> stringResource(R.string.calibrate_error_not_connected)
    CalibrationError.SEND_FAILED -> stringResource(R.string.calibrate_error_send_failed)
    CalibrationError.SEND_TIMEOUT -> stringResource(R.string.calibrate_error_send_timeout)
    CalibrationError.WATCH_NO_RESPONSE -> stringResource(R.string.calibrate_error_no_response)
    CalibrationError.SAVE_FAILED -> stringResource(R.string.calibrate_error_save_failed)
    CalibrationError.SLUMP_TOO_SIMILAR -> stringResource(R.string.calibrate_error_slouch_similar)
}
