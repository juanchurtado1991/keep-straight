package com.keepstraight.ui.desktop

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.keepstraight.R
import com.keepstraight.ui.desktop.model.CameraPermissionState
import com.keepstraight.ui.theme.PhoneDimens
import com.keepstraight.util.SystemIntentsHelper

@Composable
fun DesktopQrPermissionPrompt(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(modifier = modifier.padding(PhoneDimens.pagePadding)) {
        Text(
            text = stringResource(R.string.desktop_qr_camera_denied),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.padding(top = PhoneDimens.sectionGap),
        ) {
            Text(stringResource(R.string.desktop_qr_camera_grant))
        }
        OutlinedButton(
            onClick = {
                context.startActivity(SystemIntentsHelper.appDetailsSettings(context))
            },
            modifier = Modifier.padding(top = PhoneDimens.itemGap),
        ) {
            Text(stringResource(R.string.desktop_qr_camera_settings))
        }
    }
}

@Composable
fun rememberCameraPermissionState(): CameraPermissionState {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }
    return CameraPermissionState(
        hasPermission = hasPermission,
        requestPermission = { launcher.launch(Manifest.permission.CAMERA) },
    )
}
