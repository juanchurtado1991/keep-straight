package com.keepstraight.shared.model

import com.ghost.serialization.annotations.GhostSerialization

@GhostSerialization
data class PairedDeviceInfo(
    val watchNodeId: String,
    val pairedAt: Long,
)
