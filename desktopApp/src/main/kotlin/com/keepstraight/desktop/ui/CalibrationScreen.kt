package com.keepstraight.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keepstraight.desktop.DesktopSessionController
import com.keepstraight.shared.domain.CalibrationPhase
import com.keepstraight.shared.presentation.DesktopIssue
import com.keepstraight.shared.presentation.DesktopStatusAction

@Composable
fun CalibrationScreen(
    controller: DesktopSessionController,
    onClose: () -> Unit,
) {
    val ui by controller.uiState.collectAsState()
    val previewBitmap by controller.previewBitmap.collectAsState()
    val status by controller.statusPresentation.collectAsState()

    DisposableEffect(Unit) {
        controller.enterCalibrationUi()
        onDispose {
            controller.exitCalibrationUi()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val bitmap = previewBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Camera preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Starting camera…",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)),
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Calibrate posture",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onClose) {
                Text("Close", color = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 28.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val capturing = ui.calibrationPhase == CalibrationPhase.CAPTURE_ERECT ||
                ui.calibrationPhase == CalibrationPhase.CAPTURE_SLUMP

            Text(
                calibrationTitle(ui.calibrationPhase, ui.hasErectCapture, ui.hasCalibration, ui.issue),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                calibrationBody(
                    phase = ui.calibrationPhase,
                    hasErect = ui.hasErectCapture,
                    issue = ui.issue,
                    fallback = status.body,
                ),
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyLarge,
            )

            if (capturing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        ui.statusMessage.ifBlank { "Hold still…" },
                        color = Color.White,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    capturing -> {
                        OutlinedButton(onClick = controller::cancelCalibration) {
                            Text("Cancel")
                        }
                    }
                    // Mid dual-calibration: erect just captured (recalibrate or first run).
                    // Must come before the "complete" branch — hasCalibration stays true while
                    // recalibrating, otherwise Recalibrate only updates the erect pose.
                    ui.hasErectCapture &&
                        ui.calibrationPhase != CalibrationPhase.COMPLETE &&
                        ui.issue !is DesktopIssue.Camera &&
                        ui.issue !is DesktopIssue.ModelMissing -> {
                        Button(
                            onClick = controller::beginSlumpCalibration,
                            enabled = ui.modelReady,
                        ) { Text("Capture slumped pose") }
                        OutlinedButton(
                            onClick = controller::beginErectCalibration,
                            enabled = ui.modelReady,
                        ) { Text("Redo erect") }
                    }
                    ui.hasCalibration &&
                        ui.calibrationPhase == CalibrationPhase.COMPLETE &&
                        ui.issue == null -> {
                        Button(onClick = onClose) { Text("Done") }
                        OutlinedButton(onClick = controller::beginErectCalibration) {
                            Text("Recalibrate both")
                        }
                    }
                    else -> {
                        Button(
                            onClick = controller::beginErectCalibration,
                            enabled = ui.modelReady,
                        ) { Text("Capture erect pose") }
                        if (ui.issue is DesktopIssue.CalibrationPosesTooSimilar ||
                            ui.issue is DesktopIssue.CalibrationNoPose
                        ) {
                            // Offer slumped retry only if erect samples are still around.
                            if (ui.hasErectCapture) {
                                Button(
                                    onClick = controller::beginSlumpCalibration,
                                    enabled = ui.modelReady,
                                ) { Text("Retry slumped pose") }
                            }
                        }
                    }
                }

                status.primaryAction?.let { action ->
                    if (action == DesktopStatusAction.RETRY_CAMERA ||
                        action == DesktopStatusAction.REFRESH_CAMERAS
                    ) {
                        OutlinedButton(onClick = { controller.handleStatusAction(action) }) {
                            Text(
                                when (action) {
                                    DesktopStatusAction.RETRY_CAMERA -> "Retry camera"
                                    else -> "Refresh cameras"
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun calibrationTitle(
    phase: CalibrationPhase,
    hasErect: Boolean,
    hasCalibration: Boolean,
    issue: DesktopIssue?,
): String = when {
    issue is DesktopIssue.Camera -> "Camera problem"
    issue is DesktopIssue.ModelMissing -> "Pose model missing"
    issue is DesktopIssue.CalibrationPosesTooSimilar -> "Poses too similar"
    issue is DesktopIssue.CalibrationNoPose -> "No pose detected"
    issue is DesktopIssue.CalibrationNeedsErectFirst -> "Calibrate erect first"
    phase == CalibrationPhase.CAPTURE_ERECT -> "Hold good posture"
    phase == CalibrationPhase.CAPTURE_SLUMP -> "Hold your slouch"
    // After erect (incl. mid-recalibrate) — before "complete", which stays true until both poses save.
    hasErect && phase != CalibrationPhase.COMPLETE -> "Step 2 — Slumped posture"
    hasCalibration && phase == CalibrationPhase.COMPLETE -> "Calibration complete"
    else -> "Step 1 — Erect posture"
}

private fun calibrationBody(
    phase: CalibrationPhase,
    hasErect: Boolean,
    issue: DesktopIssue?,
    fallback: String,
): String = when {
    issue != null && issue !is DesktopIssue.TooDarkOrLowConfidence -> fallback
    phase == CalibrationPhase.CAPTURE_ERECT ->
        "Sit upright facing the camera. Stay still for a couple of seconds."
    phase == CalibrationPhase.CAPTURE_SLUMP ->
        "Round your shoulders / lean forward like you usually slouch. Stay still."
    hasErect && phase != CalibrationPhase.COMPLETE ->
        "Change to your usual slouch, then tap Capture slumped pose."
    else ->
        "Frames are not saved. Face the camera with good light."
}
