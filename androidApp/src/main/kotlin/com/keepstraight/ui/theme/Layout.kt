package com.keepstraight.ui.theme

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

/**
 * Same spacing scale as desktop [DesktopDimens] — One UI 8 calm desk/phone chrome.
 */
object PhoneDimens {
    val pagePadding: Dp = 28.dp
    val sectionGap: Dp = 16.dp
    val cardGap: Dp = 12.dp
    val cardPadding: Dp = 20.dp
    val rowGap: Dp = 10.dp
    val itemGap: Dp = 8.dp
    val radiusLarge: Dp = 28.dp
    val radiusMedium: Dp = 20.dp
    val radiusSmall: Dp = 14.dp

    object StatusPanel {
        val iconCircleSize: Dp = 96.dp
        val progressIndicatorSize: Dp = 40.dp
        val iconSize: Dp = 44.dp
        val dotSize: Dp = 18.dp
        val progressStrokeWidth: Dp = 3.dp
        val inlineProgressStrokeWidth: Dp = 2.dp
    }

    object History {
        val eventIconSize: Dp = 28.dp
    }

    object Onboarding {
        val inlineProgressSize: Dp = 16.dp
        val inlineProgressStrokeWidth: Dp = 2.dp
    }

    object Chart {
        val height: Dp = 132.dp
        val legendDotSize: Dp = 10.dp
        val legendGap: Dp = 6.dp
    }
}

/** Full-page light canvas with symmetric padding and scroll (matches DesktopPage). */
@Composable
fun PhonePage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(PhoneDimens.pagePadding),
    verticalGap: Dp = PhoneDimens.sectionGap,
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

/** One UI–style white rounded card (matches DesktopCard). */
@Composable
fun PhoneCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(PhoneDimens.cardPadding),
    verticalGap: Dp = PhoneDimens.cardGap,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PhoneDimens.radiusLarge))
            .background(containerColor)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(verticalGap),
        content = content,
    )
}

@Composable
fun phonePrimaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
fun phoneSecondaryButtonColors() = ButtonDefaults.outlinedButtonColors(
    contentColor = MaterialTheme.colorScheme.onSurface,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

fun phoneCardShape() = RoundedCornerShape(PhoneDimens.radiusLarge)
fun phoneButtonShape() = RoundedCornerShape(PhoneDimens.radiusMedium)
fun phoneChipShape() = RoundedCornerShape(PhoneDimens.radiusSmall)
