package com.keepstraight.ui.desktop.model

data class CameraPermissionState(
    val hasPermission: Boolean,
    val requestPermission: () -> Unit,
)
