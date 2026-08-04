package com.keepstraight.desktop.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.awt.Color
import java.awt.image.BufferedImage

object QrCodeBitmap {
    fun encode(content: String, sizePx: Int = 280): ImageBitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val img = BufferedImage(sizePx, sizePx, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until sizePx) {
            for (x in 0 until sizePx) {
                img.setRGB(x, y, if (matrix.get(x, y)) Color.BLACK.rgb else Color.WHITE.rgb)
            }
        }
        return img.toSkiaImageBitmap()
    }

    private fun BufferedImage.toSkiaImageBitmap(): ImageBitmap {
        val w = width
        val h = height
        val pixels = IntArray(w * h)
        getRGB(0, 0, w, h, pixels, 0, w)
        val bytes = ByteArray(w * h * 4)
        for (i in pixels.indices) {
            val p = pixels[i]
            val o = i * 4
            bytes[o] = (p shr 16 and 0xFF).toByte()
            bytes[o + 1] = (p shr 8 and 0xFF).toByte()
            bytes[o + 2] = (p and 0xFF).toByte()
            bytes[o + 3] = (p shr 24 and 0xFF).toByte()
        }
        val image = Image.makeRaster(
            ImageInfo(w, h, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL),
            bytes,
            w * 4,
        )
        return image.toComposeImageBitmap()
    }
}
