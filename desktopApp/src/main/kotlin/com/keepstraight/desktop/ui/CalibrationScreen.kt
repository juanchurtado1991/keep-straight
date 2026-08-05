package com.keepstraight.desktop.ui

import com.keepstraight.desktop.ui.i18n.DesktopStrings
import com.keepstraight.desktop.ui.i18n.StatusCopyResolver
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
import com.keepstraight.desktop.ui.home.desktopActionLabel
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
                contentDescription = DesktopStrings.calibrationPreviewCd(),
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
                    DesktopStrings.calibrationStartingCamera(),
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
                DesktopStrings.calibrationTitle(),
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onClose) {
                Text(DesktopStrings.actionClose(), color = Color.White)
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
                    fallback = StatusCopyResolver.presentationBody(status),
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
                        StatusCopyResolver.text(ui.statusKey, *ui.statusArgs.toTypedArray()),
                        color = Color.White,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    capturing -> {
                        OutlinedButton(onClick = controller::cancelCalibration) {
                            Text(DesktopStrings.actionCancel())
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
                        ) { Text(DesktopStrings.actionCaptureSlumpedPose()) }
                        OutlinedButton(
                            onClick = controller::beginErectCalibration,
                            enabled = ui.modelReady,
                        ) { Text(DesktopStrings.actionRedoErect()) }
                    }
                    ui.hasCalibration &&
                        ui.calibrationPhase == CalibrationPhase.COMPLETE &&
                        ui.issue == null -> {
                        Button(onClick = onClose) { Text(DesktopStrings.actionDone()) }
                        OutlinedButton(onClick = controller::beginErectCalibration) {
                            Text(DesktopStrings.actionRecalibrateBoth())
                        }
                    }
                    else -> {
                        Button(
                            onClick = controller::beginErectCalibration,
                            enabled = ui.modelReady,
                        ) { Text(DesktopStrings.actionCaptureErectPose()) }
                        if (ui.issue is DesktopIssue.CalibrationPosesTooSimilar ||
                            ui.issue is DesktopIssue.CalibrationNoPose
                        ) {
                            if (ui.hasErectCapture) {
                                Button(
                                    onClick = controller::beginSlumpCalibration,
                                    enabled = ui.modelReady,
                                ) { Text(DesktopStrings.actionRetrySlumpedPose()) }
                            }
                        }
                    }
                }

                status.primaryAction?.let { action ->
                    if (action == DesktopStatusAction.RETRY_CAMERA ||
                        action == DesktopStatusAction.REFRESH_CAMERAS
                    ) {
                        OutlinedButton(onClick = { controller.handleStatusAction(action) }) {
                            Text(desktopActionLabel(action))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun calibrationTitle(
    phase: CalibrationPhase,
    hasErect: Boolean,
    hasCalibration: Boolean,
    issue: DesktopIssue?,
): String = when {
    issue is DesktopIssue.Camera -> DesktopStrings.calibrationIssueCamera()
    issue is DesktopIssue.ModelMissing -> DesktopStrings.calibrationIssueModelMissing()
    issue is DesktopIssue.CalibrationPosesTooSimilar -> DesktopStrings.calibrationIssuePosesSimilar()
    issue is DesktopIssue.CalibrationNoPose -> DesktopStrings.calibrationIssueNoPose()
    issue is DesktopIssue.CalibrationNeedsErectFirst -> DesktopStrings.calibrationIssueNeedsErect()
    phase == CalibrationPhase.CAPTURE_ERECT -> DesktopStrings.calibrationPhaseHoldGood()
    phase == CalibrationPhase.CAPTURE_SLUMP -> DesktopStrings.calibrationPhaseHoldSlouch()
    hasErect && phase != CalibrationPhase.COMPLETE -> DesktopStrings.calibrationStep2Slumped()
    hasCalibration && phase == CalibrationPhase.COMPLETE -> DesktopStrings.calibrationComplete()
    else -> DesktopStrings.calibrationStep1Erect()
}

@Composable
private fun calibrationBody(
    phase: CalibrationPhase,
    hasErect: Boolean,
    issue: DesktopIssue?,
    fallback: String,
): String = when {
    issue != null && issue !is DesktopIssue.TooDarkOrLowConfidence -> fallback
    phase == CalibrationPhase.CAPTURE_ERECT ->
        DesktopStrings.calibrationBodyCaptureErect()
    phase == CalibrationPhase.CAPTURE_SLUMP ->
        DesktopStrings.calibrationBodyCaptureSlump()
    hasErect && phase != CalibrationPhase.COMPLETE ->
        DesktopStrings.calibrationBodyAfterErect()
    else ->
        DesktopStrings.calibrationBodyDefault()
}
