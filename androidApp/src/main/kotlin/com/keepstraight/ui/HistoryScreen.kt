package com.keepstraight.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.keepstraight.R
import com.keepstraight.data.local.PostureEventEntity
import com.keepstraight.ui.components.KeepStraightTopBar
import com.keepstraight.ui.history.HistoryEventRow
import com.keepstraight.ui.history.historyDayLabel
import com.keepstraight.ui.history.startOfDayMillis
import com.keepstraight.ui.theme.PhoneCard
import com.keepstraight.ui.theme.PhoneDimens

@Composable
fun HistoryScreen(
    events: LazyPagingItems<PostureEventEntity>,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            KeepStraightTopBar(
                title = stringResource(R.string.history_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        when {
            events.loadState.refresh is LoadState.Loading && events.itemCount == 0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            events.loadState.refresh is LoadState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(PhoneDimens.pagePadding),
                ) {
                    PhoneCard {
                        Text(
                            text = stringResource(R.string.history_error),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            events.itemCount == 0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(PhoneDimens.pagePadding),
                ) {
                    PhoneCard {
                        Text(
                            text = stringResource(R.string.history_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(PhoneDimens.pagePadding),
                    verticalArrangement = Arrangement.spacedBy(PhoneDimens.sectionGap),
                ) {
                    items(
                        count = events.itemCount,
                        key = events.itemKey { it.id },
                    ) { index ->
                        val event = events[index] ?: return@items
                        val previous = if (index > 0) events[index - 1] else null
                        val currentDay = startOfDayMillis(event.timestamp)
                        val previousDay = previous?.let { startOfDayMillis(it.timestamp) }
                        val showHeader = currentDay != previousDay

                        Column(verticalArrangement = Arrangement.spacedBy(PhoneDimens.itemGap)) {
                            if (showHeader) {
                                Text(
                                    text = historyDayLabel(currentDay, event.timestamp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            PhoneCard {
                                HistoryEventRow(event)
                            }
                        }
                    }
                }
            }
        }
    }
}
