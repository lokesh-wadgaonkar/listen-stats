package com.listenstats.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.listenstats.app.data.HistoryItem

@Composable
fun HistoryScreen(vm: MainViewModel) {
    val groups = vm.history.groupBy { it.dayKey }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dim.screenPadding,
            end = Dim.screenPadding,
            bottom = Dim.gap
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ScreenTitle("History")
        }
        if (groups.isEmpty()) {
            item {
                EmptyNote("No plays saved yet. A play is saved after 30 seconds of listening.")
            }
        }
        for ((dayKey, rows) in groups) {
            item {
                DayHeader(dayKey = dayKey, totalMs = rows.sumOf { it.listenedMs })
            }
            items(rows) { row ->
                HistoryRowCard(row)
            }
        }
    }
}

@Composable
private fun DayHeader(dayKey: Int, totalMs: Long) {
    Column(modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)) {
        Text(
            text = Fmt.dayHeader(dayKey),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = Fmt.duration(totalMs) + " listened",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryRowCard(row: HistoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (row.album.isBlank()) row.artist else row.artist + " · " + row.album,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Fmt.timeOfDay(row.timestamp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = Fmt.duration(row.listenedMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
