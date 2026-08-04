package com.keepstraight.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One UI 8–inspired light palette. Desktop always stays light for a calm desk product —
 * never follows the OS dark mode.
 */
private val OneUiColors = lightColorScheme(
    primary = Color(0xFF0381FE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F3FF),
    onPrimaryContainer = Color(0xFF00315C),
    secondary = Color(0xFF8C8C8C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2F2F2),
    onSecondaryContainer = Color(0xFF010102),
    background = Color(0xFFF6F6F6),
    onBackground = Color(0xFF010102),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF010102),
    onSurfaceVariant = Color(0xFF8C8C8C),
    surfaceVariant = Color(0xFFF2F2F2),
    outline = Color(0xFFE4E4E4),
    outlineVariant = Color(0xFFEEEEEE),
    error = Color(0xFFE53935),
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFF8A1C1C),
)

/** Symmetric spacing scale used across Home, Settings, and wizard. */
object DesktopDimens {
    val pagePadding: Dp = 28.dp
    val sectionGap: Dp = 16.dp
    val cardGap: Dp = 12.dp
    val cardPadding: Dp = 20.dp
    val rowGap: Dp = 10.dp
    val itemGap: Dp = 8.dp
    val radiusLarge: Dp = 28.dp
    val radiusMedium: Dp = 20.dp
    val radiusSmall: Dp = 14.dp
}

private val DesktopTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.4).sp,
        lineHeight = 40.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = (-0.2).sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
)

val StatusGood = Color(0xFF1AAB4A)
val StatusGoodContainer = Color(0xFFE5F7EB)
val StatusWarning = Color(0xFFE68A00)
val StatusWarningContainer = Color(0xFFFFF4E0)

@Composable
fun KeepStraightDesktopTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = OneUiColors,
        typography = DesktopTypography,
        content = content,
    )
}

/** Full-page light canvas with symmetric padding and scroll. */
@Composable
fun DesktopPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(DesktopDimens.pagePadding),
    verticalGap: Dp = DesktopDimens.sectionGap,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(verticalGap),
        content = content,
    )
}

/** One UI–style white rounded card. */
@Composable
fun DesktopCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesktopDimens.radiusLarge))
            .background(containerColor)
            .padding(DesktopDimens.cardPadding),
        verticalArrangement = Arrangement.spacedBy(DesktopDimens.cardGap),
        content = content,
    )
}

@Composable
fun desktopPrimaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
fun desktopSecondaryButtonColors() = ButtonDefaults.outlinedButtonColors(
    contentColor = MaterialTheme.colorScheme.onSurface,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)
