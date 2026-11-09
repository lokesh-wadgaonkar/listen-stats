package com.listenstats.app.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.listenstats.app.data.AppEntry
import com.listenstats.app.data.RankItem
import com.listenstats.app.data.StatRange
import com.listenstats.app.recorder.ListenerService
import com.listenstats.app.recorder.LiveState
import kotlinx.coroutines.delay

private val hourAxis = listOf("12 AM", "6 AM", "12 PM", "6 PM", "11 PM")

private fun RankItem?.summary(): String =
    if (this == null) "No plays yet" else Fmt.duration(listenedMs) + " · " + Fmt.plays(plays)

private fun rangeSentence(r: StatRange): String = when (r) {
    StatRange.TODAY -> "Listened today"
    StatRange.WEEK -> "Listened in the last 7 days"
    StatRange.MONTH -> "Listened this month"
    StatRange.YEAR -> "Listened this year"
    StatRange.ALL -> "Listened all time"
}

@Composable
fun OverviewScreen(vm: MainViewModel) {
    val data = vm.overview
    val range = vm.range

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dim.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Dim.gap)
    ) {
        ScreenTitle("Overview")
        LiveCard(apps = vm.apps, minSeconds = vm.minSeconds)
        RangeSelector(selected = range, onSelect = { vm.setRange(it) })

        if (data == null) {
            EmptyNote("Loading…")
        } else {
            val song = data.topSong
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = rangeSentence(range),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = Fmt.duration(data.totals.listenedMs),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = Fmt.plays(data.totals.plays) + " · " +
                            Fmt.minutes(data.totals.listenedMs) + " minutes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
            }

            HighlightCard(
                label = "TOP ARTIST",
                value = data.topArtist?.name ?: "—",
                sub = data.topArtist.summary(),
                container = MaterialTheme.colorScheme.tertiaryContainer,
                onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            )
            HighlightCard(
                label = "TOP SONG",
                value = song?.name ?: "—",
                sub = if (song == null) "No plays yet" else song.sub + " · " + song.summary(),
                container = MaterialTheme.colorScheme.secondaryContainer,
                onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.fillMaxWidth()
            )
            HighlightCard(
                label = "TOP ALBUM",
                value = data.topAlbum?.name ?: "—",
                sub = data.topAlbum.summary(),
                container = MaterialTheme.colorScheme.primaryContainer,
                onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth()
            )

            SectionCard(title = "Listening by hour of day") {
                BarChart(
                    values = data.hourValues,
                    axisLabels = hourAxis,
                    highlightIndex = data.hourPeak
                )
            }
            SectionCard(title = data.chartTitle) {
                BarChart(
                    values = data.chartValues,
                    axisLabels = data.chartLabels,
                    highlightIndex = data.chartPeak
                )
            }
        }
        Spacer(Modifier.height(Dim.gap))
    }
}

/** Shows what the recorder sees right now, so you can verify recording works. */
@Composable
private fun LiveCard(apps: List<AppEntry>, minSeconds: Int) {
    val connected by LiveState.connected.collectAsState()
    val sessions by LiveState.sessions.collectAsState()

    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            tick = SystemClock.elapsedRealtime()
        }
    }
    val now = if (tick == 0L) SystemClock.elapsedRealtime() else tick
    val neededMs = minSeconds * 1000L

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(Dim.cardPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Recorder",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!connected) {
                Text(
                    text = "Not connected yet. Open this app once after granting access. If it " +
                        "stays like this, check the battery settings in Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else if (sessions.isEmpty()) {
                Text(
                    text = "Running. Start a song in Poweramp and it will show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                for (s in sessions) {
                    val entry = apps.firstOrNull { it.packageName == s.packageName }
                    val label = entry?.label ?: s.packageName
                    val appOn = entry?.enabled ?: (s.packageName == ListenerService.POWERAMP)
                    val playing = s.playingSince >= 0L
                    val listened = s.accumulatedMs + if (playing) now - s.playingSince else 0L
                    val status = when {
                        !appOn -> "This app is turned off in Settings, so it is not saved."
                        s.artist.isBlank() -> "No artist tag, so this will not be saved."
                        listened >= neededMs -> "Counted. It will be saved when the track ends."
                        else -> "Counting " + Fmt.clock(listened) + " of " + Fmt.clock(neededMs)
                    }
                    Text(
                        text = label + " · " + (if (playing) "Playing" else "Paused"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = s.title + if (s.artist.isBlank()) "" else " — " + s.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
