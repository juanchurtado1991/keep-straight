package com.keepstraight.desktop.ui.theme

import androidx.compose.ui.unit.dp
import com.keepstraight.sharedui.theme.SharedDimens

/** Symmetric spacing scale used across Home, Settings, and wizard. */
object DesktopDimens {
    val pagePadding = SharedDimens.pagePadding
    val sectionGap = SharedDimens.sectionGap
    val cardGap = SharedDimens.cardGap
    val cardPadding = SharedDimens.cardPadding
    val rowGap = SharedDimens.rowGap
    val itemGap = SharedDimens.itemGap
    val radiusLarge = SharedDimens.radiusLarge
    val radiusMedium = SharedDimens.radiusMedium
    val radiusSmall = SharedDimens.radiusSmall

    object StatusCard {
        val dotSize = 12.dp
        val progressSize = 18.dp
        val progressStrokeWidth = 2.dp
    }
}
