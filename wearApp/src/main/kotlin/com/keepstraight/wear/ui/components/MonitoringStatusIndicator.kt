package com.keepstraight.wear.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.keepstraight.wear.R
import com.keepstraight.wear.ui.model.WatchStatusPresentation
import com.keepstraight.wear.ui.theme.WearColors
import com.keepstraight.wear.ui.theme.WearDimens
import com.keepstraight.wear.ui.theme.WearTypography

@Composable
fun MonitoringStatusIndicator(
    presentation: WatchStatusPresentation,
    flashVisible: Boolean,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(
            horizontal = WearDimens.screenPaddingHorizontal,
            vertical = WearDimens.screenPaddingVertical,
        ),
    ) {
        Box(
            modifier = Modifier
                .size(WearDimens.statusDotSize)
                .clip(CircleShape)
                .background(if (flashVisible) WearColors.flashText else presentation.accent),
        )
        Spacer(modifier = Modifier.height(WearDimens.titleGap))
        Text(
            text = stringResource(R.string.app_name),
            color = when {
                flashVisible -> WearColors.flashTextMuted
                else -> MaterialTheme.colors.onBackground.copy(alpha = WearColors.onBackgroundMuted)
            },
            textAlign = TextAlign.Center,
            fontSize = WearTypography.appName,
        )
        Spacer(modifier = Modifier.height(WearDimens.headlineGap))
        Text(
            text = stringResource(presentation.titleRes),
            textAlign = TextAlign.Center,
            color = if (flashVisible) WearColors.flashText else MaterialTheme.colors.onBackground,
            fontSize = WearTypography.title,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
        )
        Spacer(modifier = Modifier.height(WearDimens.hintGap))
        Text(
            text = stringResource(presentation.hintRes),
            textAlign = TextAlign.Center,
            color = when {
                flashVisible -> WearColors.flashTextHint
                else -> MaterialTheme.colors.onBackground.copy(alpha = WearColors.onBackgroundHint)
            },
            fontSize = WearTypography.hint,
            maxLines = 3,
        )
    }
}
