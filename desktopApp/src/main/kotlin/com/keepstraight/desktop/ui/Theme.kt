package com.keepstraight.desktop.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.keepstraight.desktop.ui.theme.DesktopCard as ThemeDesktopCard
import com.keepstraight.desktop.ui.theme.DesktopPage as ThemeDesktopPage
import com.keepstraight.desktop.ui.theme.KeepStraightDesktopTheme as ThemeRoot

typealias DesktopDimens = com.keepstraight.desktop.ui.theme.DesktopDimens

val StatusGood: Color = com.keepstraight.desktop.ui.theme.StatusGood
val StatusGoodContainer: Color = com.keepstraight.desktop.ui.theme.StatusGoodContainer
val StatusWarning: Color = com.keepstraight.desktop.ui.theme.StatusWarning
val StatusWarningContainer: Color = com.keepstraight.desktop.ui.theme.StatusWarningContainer

@Composable
fun KeepStraightDesktopTheme(content: @Composable () -> Unit) = ThemeRoot(content)

@Composable
fun DesktopPage(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(DesktopDimens.pagePadding),
    verticalGap: Dp = DesktopDimens.sectionGap,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) = ThemeDesktopPage(modifier, contentPadding, verticalGap, content)

@Composable
fun DesktopCard(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    containerColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) = ThemeDesktopCard(modifier, containerColor, content)

@Composable
fun desktopPrimaryButtonColors() = com.keepstraight.desktop.ui.theme.desktopPrimaryButtonColors()

@Composable
fun desktopSecondaryButtonColors() = com.keepstraight.desktop.ui.theme.desktopSecondaryButtonColors()
