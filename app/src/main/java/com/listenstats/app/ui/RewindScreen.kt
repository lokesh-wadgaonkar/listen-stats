package com.listenstats.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private class Story(
    val kicker: String,
    val big: String,
    val small: String,
    val isNumber: Boolean
)

private fun buildStories(d: RewindData): List<Story> {
    val out = ArrayList<Story>()
    out.add(
        Story(
            "Your " + d.title,
            Fmt.minutes(d.totalMs),
            "minutes of music\n" + Fmt.plays(d.plays),
            true
        )
    )
    val artist = d.topArtist
    if (artist != null) {
        out.add(
            Story(
                "Top artist",
                artist.name,
                Fmt.duration(artist.listenedMs) + " · " + Fmt.plays(artist.plays),
                false
            )
        )
    }
    val song = d.topSong
    if (song != null) {
        out.add(
            Story(
                "Top song",
                song.name,
                song.sub + "\n" + Fmt.duration(song.listenedMs) + " · " + Fmt.plays(song.plays),
                false
            )
        )
    }
    val album = d.topAlbum
    if (album != null) {
        out.add(
            Story(
                "Top album",
                album.name,
                Fmt.duration(album.listenedMs) + " · " + Fmt.plays(album.plays),
                false
            )
        )
    }
    if (d.favHour >= 0) {
        out.add(
            Story(
                "Favorite hour",
                Fmt.hourLabel(d.favHour),
                "You listened " + Fmt.duration(d.favHourMs) + " in that hour",
                true
            )
        )
    }
    if (d.longestDayKey != 0) {
        out.add(
            Story(
                "Longest listening day",
                Fmt.duration(d.longestDayMs),
                Fmt.dayLong(d.longestDayKey),
                true
            )
        )
    }
    return out
}

@Composable
fun RewindScreen(vm: MainViewModel) {
    val data = vm.rewind
    val isYear = vm.rewindIsYear
    val keys = if (isYear) vm.rewindYears else vm.rewindMonths
    val selected = vm.rewindSelected

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dim.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Dim.gap)
    ) {
        ScreenTitle("Rewind")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !isYear,
                onClick = { vm.setRewindMode(false) },
                label = { Text("Month", style = MaterialTheme.typography.labelLarge) },
                shape = RoundedCornerShape(50)
            )
            FilterChip(
                selected = isYear,
                onClick = { vm.setRewindMode(true) },
                label = { Text("Year", style = MaterialTheme.typography.labelLarge) },
                shape = RoundedCornerShape(50)
            )
        }

        if (keys.isEmpty() || data == null) {
            EmptyNote("Rewind appears once you have saved some plays.")
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (k in keys) {
                    FilterChip(
                        selected = k == selected,
                        onClick = { vm.selectRewind(k) },
                        label = {
                            Text(
                                text = if (isYear) k.toString() else Fmt.monthLong(k),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        shape = RoundedCornerShape(50)
                    )
                }
            }
            StoryPager(data)
        }
        Spacer(Modifier.height(Dim.gap))
    }
}

@Composable
private fun StoryPager(d: RewindData) {
    val stories = buildStories(d)
    val pagerState = rememberPagerState(pageCount = { stories.size })

    LaunchedEffect(d.title) {
        pagerState.scrollToPage(0)
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth(),
        pageSpacing = 12.dp
    ) { page ->
        StoryCard(story = stories[page], index = page)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in stories.indices) {
            val active = i == pagerState.currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(if (active) 24.dp else 8.dp)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Composable
private fun StoryCard(story: Story, index: Int) {
    val pair = StoryColors.gradients[index % StoryColors.gradients.size]
    val white = StoryColors.onStory

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dim.storyHeight)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.verticalGradient(listOf(pair.first, pair.second)))
            .padding(28.dp)
    ) {
        Text(
            text = story.kicker.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = white.copy(alpha = 0.85f),
            modifier = Modifier.align(Alignment.TopStart)
        )
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text(
                text = story.big,
                style = if (story.isNumber) MaterialTheme.typography.displayLarge
                else MaterialTheme.typography.displaySmall,
                color = white,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = story.small,
                style = MaterialTheme.typography.titleLarge,
                color = white.copy(alpha = 0.9f)
            )
        }
        Text(
            text = "Listen Stats",
            style = MaterialTheme.typography.labelMedium,
            color = white.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}
