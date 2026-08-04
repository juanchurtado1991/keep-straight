package com.keepstraight.desktop

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter

/** Matches androidApp adaptive icon: teal #2E7D6F + white ring and plus. */
private val LauncherTeal = Color(0xFF2E7D6F)

val TrayIcon: Painter = object : Painter() {
    override val intrinsicSize: Size = Size(64f, 64f)

    override fun DrawScope.onDraw() {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = LauncherTeal,
            cornerRadius = CornerRadius(w * 0.22f, h * 0.22f),
        )
        val stroke = w * (3f / 108f)
        val cx = w * 0.5f
        val cy = h * 0.5f
        val outer = w * (30f / 108f)
        val inner = w * (18f / 108f)
        drawCircle(
            color = Color.White,
            radius = (outer + inner) / 2f,
            center = Offset(cx, cy),
            style = Stroke(width = (outer - inner).coerceAtLeast(1f)),
        )
        // Plus (viewport coords from ic_launcher_foreground.xml)
        val halfT = stroke / 2f
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(cx - halfT, h * (42f / 108f)),
            size = Size(stroke, h * (12f / 108f)),
            cornerRadius = CornerRadius(halfT, halfT),
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * (48f / 108f), cy - halfT),
            size = Size(w * (12f / 108f), stroke),
            cornerRadius = CornerRadius(halfT, halfT),
        )
    }
}
