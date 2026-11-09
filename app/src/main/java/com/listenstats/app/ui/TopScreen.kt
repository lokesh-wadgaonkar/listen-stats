package com.listenstats.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.listenstats.app.data.RankItem

private val tabTitles = listOf("Artists", "Songs", "Albums")

@Composable
fun TopScreen(vm: MainViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val data = vm.top
    val list: List<RankItem> = when (tab) {
        0 -> data.artists
        1 -> data.songs
        else -> data.albums
    }
    val maxMs = if (list.isEmpty()) 0L else list[0].listenedMs

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dim.screenPadding,
            end = Dim.screenPadding,
            bottom = Dim.gap
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenTitle("Top")
        }
        item {
            RangeSelector(selected = vm.range, onSelect = { vm.setRange(it) })
        }
        item {
            TabRow(
                selectedTabIndex = tab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                for (i in tabTitles.indices) {
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        text = {
                            Text(
                                text = tabTitles[i],
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    )
                }
            }
        }
        item {
            Spacer(Modifier.height(2.dp))
        }
        if (list.isEmpty()) {
            item {
                EmptyNote("Nothing here yet for this range.")
            }
        } else {
            itemsIndexed(list) { index, entry ->
                RankRow(
                    position = index + 1,
                    item = entry,
                    maxMs = maxMs,
                    showSub = tab != 0
                )
            }
        }
    }
}
