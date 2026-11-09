package com.listenstats.app.data

/** One saved listening row. All keys are derived from [timestamp] (local time). */
data class PlayRow(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val listenedMs: Long,
    val timestamp: Long,
    val packageName: String,
    val dayKey: Int,
    val monthKey: Int,
    val year: Int,
    val hourOfDay: Int
)

data class Totals(val listenedMs: Long, val plays: Int)

data class RankItem(val name: String, val sub: String, val listenedMs: Long, val plays: Int)

data class HistoryItem(
    val title: String,
    val artist: String,
    val album: String,
    val listenedMs: Long,
    val timestamp: Long,
    val packageName: String,
    val dayKey: Int
)

data class AppEntry(val packageName: String, val label: String, val enabled: Boolean)

enum class StatRange(val label: String) {
    TODAY("Today"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    ALL("All time")
}

/** Half-open time window: from <= timestamp < to. */
data class TimeSpan(val from: Long, val to: Long)

/**
 * Snapshot of one tracked media session, for the live card in the UI.
 * [playingSince] is a SystemClock.elapsedRealtime() value, or -1 when not playing.
 */
data class LiveSession(
    val packageName: String,
    val title: String,
    val artist: String,
    val accumulatedMs: Long,
    val playingSince: Long
)
