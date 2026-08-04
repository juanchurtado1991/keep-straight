package com.keepstraight.desktop.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.keepstraight.shared.vision.CameraFrame
import java.awt.image.BufferedImage

object FramePreview {
    /** Downscale for UI to keep RAM/CPU low. */
    fun toImageBitmap(frame: CameraFrame, maxWidth: Int = 480): ImageBitmap {
        val scale = if (frame.width > maxWidth) maxWidth.toFloat() / frame.width else 1f
        val w = (frame.width * scale).toInt().coerceAtLeast(1)
        val h = (frame.height * scale).toInt().coerceAtLeast(1)
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until h) {
            val sy = (y / scale).toInt().coerceIn(0, frame.height - 1)
            for (x in 0 until w) {
                val sx = (x / scale).toInt().coerceIn(0, frame.width - 1)
                val src = (sy * frame.width + sx) * 3
                val r = frame.rgb[src].toInt() and 0xFF
                val g = frame.rgb[src + 1].toInt() and 0xFF
                val b = frame.rgb[src + 2].toInt() and 0xFF
                img.setRGB(x, y, (r shl 16) or (g shl 8) or b)
            }
        }
        return img.toComposeImageBitmap()
    }
}
