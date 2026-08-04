package com.keepstraight.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val OneUiShapes = Shapes(
    extraSmall = RoundedCornerShape(PhoneDimens.radiusSmall),
    small = RoundedCornerShape(PhoneDimens.radiusSmall),
    medium = RoundedCornerShape(PhoneDimens.radiusMedium),
    large = RoundedCornerShape(PhoneDimens.radiusLarge),
    extraLarge = RoundedCornerShape(PhoneDimens.radiusLarge),
)

/** Always-light scheme — mirrors desktop [KeepStraightDesktopTheme]. */
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
)

@Composable
fun KeepStraightTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = OneUiShapes,
        content = content,
    )
}
