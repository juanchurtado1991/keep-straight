package com.keepstraight.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.keepstraight.R
import com.keepstraight.bridge.PhoneLanBridgeService
import com.keepstraight.presentation.pairing.DesktopPairingPhase
import com.keepstraight.presentation.pairing.DesktopPairingViewModel
import com.keepstraight.ui.components.KeepStraightTopBar
import com.keepstraight.util.SystemIntentsHelper
import java.util.concurrent.Executors

@Composable
fun DesktopQrScanScreen(
    viewModel: DesktopPairingViewModel,
    onBack: () -> Unit,
    onPaired: () -> Unit,
) {
    val context = LocalContext.current
    val pairingState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        PhoneLanBridgeService.start(context)
    }

    LaunchedEffect(pairingState.phase) {
        if (pairingState.phase == DesktopPairingPhase.SUCCESS) {
            onPaired()
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val statusText = when (pairingState.phase) {
        DesktopPairingPhase.PAIRING -> stringResource(R.string.desktop_qr_pairing)
        DesktopPairingPhase.SUCCESS -> stringResource(R.string.desktop_qr_success)
        DesktopPairingPhase.FAILED ->
            pairingState.errorMessage ?: stringResource(R.string.desktop_qr_failed)
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
            if (hasCameraPermission) {
                CameraQrPreview(
                    modifier = Modifier.fillMaxSize(),
                    enabled = pairingState.phase != DesktopPairingPhase.PAIRING,
                    onQr = viewModel::onQrPayload,
                )
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                ) {
                    Text(
                        text = stringResource(R.string.desktop_qr_camera_denied),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        Text(stringResource(R.string.desktop_qr_camera_grant))
                    }
                    OutlinedButton(
                        onClick = {
                            context.startActivity(SystemIntentsHelper.appDetailsSettings(context))
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(stringResource(R.string.desktop_qr_camera_settings))
                    }
                }
            }
            if (hasCameraPermission) {
                Text(
                    text = statusText ?: stringResource(R.string.desktop_qr_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp),
                )
            }
            if (pairingState.phase == DesktopPairingPhase.PAIRING) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun CameraQrPreview(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onQr: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener(
                {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        if (!enabled) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val media = imageProxy.image
                        if (media != null) {
                            val yPlane = media.planes[0]
                            val yBuffer = yPlane.buffer
                            val rowStride = yPlane.rowStride
                            val width = imageProxy.width
                            val height = imageProxy.height
                            val yBytes = ByteArray(width * height)
                            if (rowStride == width) {
                                yBuffer.get(yBytes)
                            } else {
                                var dest = 0
                                for (row in 0 until height) {
                                    yBuffer.position(row * rowStride)
                                    yBuffer.get(yBytes, dest, width)
                                    dest += width
                                }
                            }
                            val source = PlanarYUVLuminanceSource(
                                yBytes,
                                width,
                                height,
                                0,
                                0,
                                width,
                                height,
                                false,
                            )
                            val bitmap = BinaryBitmap(HybridBinarizer(source))
                            runCatching {
                                val result = MultiFormatReader().decode(bitmap)
                                onQr(result.text)
                            }
                        }
                        imageProxy.close()
                    }
                    runCatching {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                    }
                },
                ContextCompat.getMainExecutor(ctx),
            )
            previewView
        },
    )
}
