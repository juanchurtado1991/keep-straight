package com.keepstraight.desktop.ui.theme

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

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

fun desktopCardShape() = RoundedCornerShape(DesktopDimens.radiusLarge)
fun desktopButtonShape() = RoundedCornerShape(DesktopDimens.radiusMedium)
fun desktopChipShape() = RoundedCornerShape(DesktopDimens.radiusSmall)
