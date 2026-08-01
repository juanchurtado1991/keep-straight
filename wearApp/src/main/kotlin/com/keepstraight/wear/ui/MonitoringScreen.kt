package com.keepstraight.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.keepstraight.wear.R

@Composable
fun MonitoringScreen(
    statusText: String,
    flashVisible: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (flashVisible) {
                    Modifier.background(Color.White.copy(alpha = 0.85f))
                } else {
                    Modifier.background(MaterialTheme.colors.background)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.title2,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onBackground,
            )
        }
    }
}
