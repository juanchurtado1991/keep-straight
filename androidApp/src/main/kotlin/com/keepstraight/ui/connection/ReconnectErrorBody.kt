package com.keepstraight.ui.connection

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.keepstraight.R
import com.keepstraight.shared.presentation.ReconnectError

@Composable
fun reconnectErrorBody(reason: ReconnectError): String = when (reason) {
    ReconnectError.NO_PAIRED_WATCH -> stringResource(R.string.reconnect_error_no_watch)
    ReconnectError.WATCH_UNREACHABLE -> stringResource(R.string.reconnect_error_unreachable)
    ReconnectError.SEND_FAILED -> stringResource(R.string.reconnect_error_send_failed)
    ReconnectError.SEND_TIMEOUT -> stringResource(R.string.reconnect_error_timeout)
}
