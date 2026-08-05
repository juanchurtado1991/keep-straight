package com.keepstraight.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.keepstraight.R
import com.keepstraight.bridge.PhoneLanBridgeService
import com.keepstraight.presentation.pairing.DesktopPairingViewModel
import com.keepstraight.presentation.pairing.model.DesktopPairingPhase
import com.keepstraight.bridge.PhonePairError
import com.keepstraight.ui.camera.CameraQrPreview
import com.keepstraight.ui.components.KeepStraightTopBar
import com.keepstraight.ui.desktop.DesktopQrPermissionPrompt
import com.keepstraight.ui.desktop.rememberCameraPermissionState
import com.keepstraight.ui.theme.PhoneDimens
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DesktopQrScanScreen(
    viewModel: DesktopPairingViewModel,
    onBack: () -> Unit,
    onPaired: () -> Unit,
) {
    val context = LocalContext.current
    val pairingState by viewModel.state.collectAsStateWithLifecycle()
    val cameraPermission = rememberCameraPermissionState()

    LaunchedEffect(Unit) {
        PhoneLanBridgeService.start(context)
    }

    LaunchedEffect(pairingState.phase) {
        if (pairingState.phase == DesktopPairingPhase.SUCCESS) {
            onPaired()
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermission.hasPermission) {
            cameraPermission.requestPermission()
        }
    }

    val statusText = when (pairingState.phase) {
        DesktopPairingPhase.PAIRING -> stringResource(R.string.desktop_qr_pairing)
        DesktopPairingPhase.SUCCESS -> stringResource(R.string.desktop_qr_success)
        DesktopPairingPhase.FAILED -> when (pairingState.error) {
            PhonePairError.NO_WIFI -> stringResource(R.string.phone_pair_no_wifi)
            PhonePairError.DESKTOP_UNREACHABLE -> stringResource(R.string.phone_pair_desktop_unreachable)
            PhonePairError.DESKTOP_REJECTED -> stringResource(
                R.string.phone_pair_desktop_rejected,
                pairingState.errorDetail.orEmpty(),
            )
            null -> stringResource(R.string.desktop_qr_failed)
        }
        DesktopPairingPhase.INVALID_QR -> stringResource(R.string.desktop_qr_failed)
        DesktopPairingPhase.IDLE -> null
    }

    Scaffold(
        topBar = {
            KeepStraightTopBar(
                title = stringResource(R.string.desktop_qr_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (cameraPermission.hasPermission) {
                CameraQrPreview(
                    modifier = Modifier.fillMaxSize(),
                    enabled = pairingState.phase != DesktopPairingPhase.PAIRING,
                    onQr = viewModel::onQrPayload,
                )
            } else {
                DesktopQrPermissionPrompt(
                    onRequestPermission = cameraPermission.requestPermission,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            if (cameraPermission.hasPermission) {
                Text(
                    text = statusText ?: stringResource(R.string.desktop_qr_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(PhoneDimens.pagePadding),
                )
            }
            if (pairingState.phase == DesktopPairingPhase.PAIRING) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
