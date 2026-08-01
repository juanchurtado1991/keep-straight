package com.keepstraight.shared.model

import com.ghost.serialization.annotations.GhostSerialization

@GhostSerialization
data class AlertPreferences(
    val hapticEnabled: Boolean = true,
    val visualEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val phoneNotificationEnabled: Boolean = false,
)
