package com.keepstraight.shared.vision

/**
 * Lowercase substrings from OS / driver error messages (not user-visible copy).
 * Used to map native webcam failures to [CameraError].
 */
internal object JvmCameraAccessErrorSignals {
    private val permissionHints = listOf(
        "permission",
        "not authorized",
        "access is denied",
        "access denied",
        "eacces",
        "privacy",
    )

    private val inUseHints = listOf(
        "busy",
        "in use",
        "locked",
    )

    private val notFoundHints = listOf(
        "no such file",
        "no device",
        "not found",
    )

    fun classify(message: String?): CameraError? {
        val detail = message.orEmpty().lowercase()
        if (detail.isBlank()) return null
        return when {
            permissionHints.any(detail::contains) -> CameraError.PERMISSION_DENIED
            inUseHints.any(detail::contains) -> CameraError.IN_USE
            notFoundHints.any(detail::contains) -> CameraError.NOT_FOUND
            else -> null
        }
    }
}
