package com.keepstraight.sharedui.theme

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
import androidx.compose.ui.unit.dp

/** Symmetric spacing scale shared by phone and desktop Compose surfaces. */
object SharedDimens {
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

@Composable
fun SharedPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(SharedDimens.pagePadding),
    verticalGap: Dp = SharedDimens.sectionGap,
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
fun SharedCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(SharedDimens.cardPadding),
    verticalGap: Dp = SharedDimens.cardGap,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SharedDimens.radiusLarge))
            .background(containerColor)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(verticalGap),
        content = content,
    )
}

@Composable
fun sharedPrimaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
fun sharedSecondaryButtonColors() = ButtonDefaults.outlinedButtonColors(
    contentColor = MaterialTheme.colorScheme.onSurface,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

fun sharedCardShape() = RoundedCornerShape(SharedDimens.radiusLarge)
fun sharedButtonShape() = RoundedCornerShape(SharedDimens.radiusMedium)
fun sharedChipShape() = RoundedCornerShape(SharedDimens.radiusSmall)
