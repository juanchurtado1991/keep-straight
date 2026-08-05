package com.keepstraight.ui.camera

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

@Composable
fun CameraQrPreview(
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
                        .setTargetResolution(
                            Size(CameraQrConfig.ANALYSIS_WIDTH, CameraQrConfig.ANALYSIS_HEIGHT),
                        )
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
