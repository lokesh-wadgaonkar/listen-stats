package com.listenstats.app.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.listenstats.app.data.AppEntry
import com.listenstats.app.data.Dates
import com.listenstats.app.data.Db
import com.listenstats.app.data.HistoryItem
import com.listenstats.app.data.Prefs
import com.listenstats.app.data.RankItem
import com.listenstats.app.data.StatRange
import com.listenstats.app.data.TimeSpan
import com.listenstats.app.data.Totals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class OverviewData(
    val totals: Totals,
    val topArtist: RankItem?,
    val topSong: RankItem?,
    val topAlbum: RankItem?,
    val hourValues: List<Long>,
    val hourPeak: Int,
    val chartTitle: String,
    val chartValues: List<Long>,
    val chartLabels: List<String>,
    val chartPeak: Int
)

data class TopData(
    val artists: List<RankItem>,
    val songs: List<RankItem>,
    val albums: List<RankItem>
)

data class RewindData(
    val title: String,
    val totalMs: Long,
    val plays: Int,
    val topArtist: RankItem?,
    val topSong: RankItem?,
    val topAlbum: RankItem?,
    val favHour: Int,
    val favHourMs: Long,
    val longestDayKey: Int,
    val longestDayMs: Long
)

internal class ChartBuild(val title: String, val values: List<Long>, val labels: List<String>)

internal class RewindLoad(
    val months: List<Int>,
    val years: List<Int>,
    val selected: Int,
    val data: RewindData?
)

private fun peakIndex(values: List<Long>): Int {
    var best = -1
    var bestV = 0L
    for (i in values.indices) {
        if (values[i] > bestV) {
            bestV = values[i]
            best = i
        }
    }
    return best
}

class MainViewModel(private val appCtx: Application) : AndroidViewModel(appCtx) {

    private val db: Db = Db.get(appCtx)
    private val prefs: Prefs = Prefs(appCtx)

    var accessGranted by mutableStateOf(false)
        private set

    var range by mutableStateOf(StatRange.WEEK)
        private set

    var overview by mutableStateOf<OverviewData?>(null)
        private set

    var top by mutableStateOf(TopData(emptyList(), emptyList(), emptyList()))
        private set

    var history by mutableStateOf<List<HistoryItem>>(emptyList())
        private set

    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set

    var minSeconds by mutableStateOf(prefs.minSeconds)
        private set

    var amoled by mutableStateOf(prefs.amoled)
        private set

    var dynamicColor by mutableStateOf(prefs.dynamicColor)
        private set

    var message by mutableStateOf<String?>(null)
        private set

    var rewindMonths by mutableStateOf<List<Int>>(emptyList())
        private set

    var rewindYears by mutableStateOf<List<Int>>(emptyList())
        private set

    var rewindIsYear by mutableStateOf(false)
        private set

    var rewindSelected by mutableStateOf(0)
        private set

    var rewind by mutableStateOf<RewindData?>(null)
        private set

    private var statsJob: Job? = null
    private var rewindJob: Job? = null

    init {
        refreshAccess()
        viewModelScope.launch {
            Db.version.collect {
                reloadAll()
            }
        }
    }

    // ------------------------------------------------------------ access

    fun refreshAccess() {
        val granted = NotificationManagerCompat
            .getEnabledListenerPackages(appCtx)
            .contains(appCtx.packageName)
        accessGranted = granted
    }

    // ------------------------------------------------------------ loading

    private fun reloadAll() {
        loadStats()
        loadHistory()
        loadApps()
        loadRewind()
    }

    private fun loadStats() {
        val r = range
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { computeStats(r) }
            overview = result.first
            top = result.second
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { db.history(500) }
            history = list
        }
    }

    private fun loadApps() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { db.apps() }
            apps = list
        }
    }

    private fun loadRewind() {
        val isYear = rewindIsYear
        val sel = rewindSelected
        rewindJob?.cancel()
        rewindJob = viewModelScope.launch {
            val load = withContext(Dispatchers.IO) { computeRewind(isYear, sel) }
            rewindMonths = load.months
            rewindYears = load.years
            rewindSelected = load.selected
            rewind = load.data
        }
    }

    // ------------------------------------------------------------ user actions

    fun setRange(r: StatRange) {
        range = r
        loadStats()
    }

    fun setRewindMode(isYear: Boolean) {
        rewindIsYear = isYear
        rewindSelected = 0
        loadRewind()
    }

    fun selectRewind(key: Int) {
        rewindSelected = key
        loadRewind()
    }

    fun setAppEnabled(pkg: String, enabled: Boolean) {
        apps = apps.map { if (it.packageName == pkg) it.copy(enabled = enabled) else it }
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.setEnabled(pkg, enabled) }
        }
    }

    fun setMinSeconds(value: Int) {
        val v = value.coerceIn(5, 300)
        minSeconds = v
        prefs.minSeconds = v
    }

    fun setAmoled(value: Boolean) {
        amoled = value
        prefs.amoled = value
    }

    fun setDynamicColor(value: Boolean) {
        dynamicColor = value
        prefs.dynamicColor = value
    }

    fun clearAll() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.clearPlays() }
            message = "All listening data deleted"
        }
    }

    fun exportTo(uri: Uri) {
        val resolver = appCtx.contentResolver
        viewModelScope.launch {
            val msg = withContext(Dispatchers.IO) {
                try {
                    val json = db.exportJson()
                    val out = resolver.openOutputStream(uri, "wt")
                        ?: throw IllegalStateException("Could not open the file")
                    out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                    "Backup saved"
                } catch (e: Exception) {
                    "Export failed: ${e.message}"
                }
            }
            message = msg
        }
    }

    fun importFrom(uri: Uri) {
        val resolver = appCtx.contentResolver
        viewModelScope.launch {
            val msg = withContext(Dispatchers.IO) {
                try {
                    val input = resolver.openInputStream(uri)
                        ?: throw IllegalStateException("Could not open the file")
                    val text = input.use { it.readBytes().toString(Charsets.UTF_8) }
                    val added = db.importJson(text)
                    "Imported $added plays"
                } catch (e: Exception) {
                    "Import failed: ${e.message}"
                }
            }
            message = msg
        }
    }

    fun consumeMessage() {
        message = null
    }

    // ------------------------------------------------------------ computation (background thread)

    private fun computeStats(r: StatRange): Pair<OverviewData, TopData> {
        val span = Dates.spanFor(r)
        val totals = db.totals(span)
        val artists = db.topArtists(span, 100)
        val songs = db.topSongs(span, 100)
        val albums = db.topAlbums(span, 100)
        val hours = db.byHour(span)
        val chart = buildChart(r)
        val ov = OverviewData(
            totals = totals,
            topArtist = artists.firstOrNull(),
            topSong = songs.firstOrNull(),
            topAlbum = albums.firstOrNull(),
            hourValues = hours,
            hourPeak = peakIndex(hours),
            chartTitle = chart.title,
            chartValues = chart.values,
            chartLabels = chart.labels,
            chartPeak = peakIndex(chart.values)
        )
        return Pair(ov, TopData(artists, songs, albums))
    }

    private fun buildChart(r: StatRange): ChartBuild {
        val now = System.currentTimeMillis()
        val values = ArrayList<Long>()
        val labels = ArrayList<String>()
        return when (r) {
            StatRange.TODAY, StatRange.WEEK -> {
                val map = db.byDay(Dates.spanFor(StatRange.WEEK, now))
                val c = Dates.startOfDay(now)
                c.add(Calendar.DAY_OF_YEAR, -6)
                val fmt = SimpleDateFormat("EEE", Locale.getDefault())
                for (i in 0 until 7) {
                    values.add(map[Dates.dayKeyOf(c)] ?: 0L)
                    labels.add(fmt.format(c.time).take(1))
                    c.add(Calendar.DAY_OF_YEAR, 1)
                }
                ChartBuild("Last 7 days", values, labels)
            }
            StatRange.MONTH -> {
                val map = db.byDay(Dates.spanFor(StatRange.MONTH, now))
                val c = Dates.startOfDay(now)
                c.set(Calendar.DAY_OF_MONTH, 1)
                val n = c.getActualMaximum(Calendar.DAY_OF_MONTH)
                for (i in 0 until n) {
                    values.add(map[Dates.dayKeyOf(c)] ?: 0L)
                    c.add(Calendar.DAY_OF_YEAR, 1)
                }
                labels.add("1")
                labels.add(((n + 1) / 2).toString())
                labels.add(n.toString())
                ChartBuild("By day", values, labels)
            }
            StatRange.YEAR -> {
                val map = db.byMonth(Dates.spanFor(StatRange.YEAR, now))
                val year = Dates.year(now)
                for (m in 1..12) {
                    val key = year * 100 + m
                    values.add(map[key] ?: 0L)
                    labels.add(Fmt.monthShort(key).take(1))
                }
                ChartBuild("By month", values, labels)
            }
            StatRange.ALL -> {
                val c = Dates.startOfDay(now)
                c.set(Calendar.DAY_OF_MONTH, 1)
                c.add(Calendar.MONTH, -11)
                val span = TimeSpan(c.timeInMillis, Long.MAX_VALUE)
                val map = db.byMonth(span)
                for (i in 0 until 12) {
                    val key = c.get(Calendar.YEAR) * 100 + (c.get(Calendar.MONTH) + 1)
                    values.add(map[key] ?: 0L)
                    labels.add(Fmt.monthShort(key).take(1))
                    c.add(Calendar.MONTH, 1)
                }
                ChartBuild("Last 12 months", values, labels)
            }
        }
    }

    private fun computeRewind(isYear: Boolean, sel: Int): RewindLoad {
        val months = db.distinctMonths()
        val years = db.distinctYears()
        val list = if (isYear) years else months
        val chosen = if (sel != 0 && list.contains(sel)) sel else (list.firstOrNull() ?: 0)
        val data = if (chosen == 0) null else buildRewind(isYear, chosen)
        return RewindLoad(months, years, chosen, data)
    }

    private fun buildRewind(isYear: Boolean, key: Int): RewindData {
        val span = if (isYear) Dates.yearSpan(key) else Dates.monthSpan(key / 100, key % 100)
        val title = if (isYear) key.toString() else Fmt.monthLong(key)
        val totals = db.totals(span)
        val hours = db.byHour(span)
        val hourIdx = peakIndex(hours)
        val days = db.byDay(span)
        var dayKey = 0
        var dayMs = 0L
        for ((k, v) in days) {
            if (v > dayMs) {
                dayMs = v
                dayKey = k
            }
        }
        return RewindData(
            title = title,
            totalMs = totals.listenedMs,
            plays = totals.plays,
            topArtist = db.topArtists(span, 1).firstOrNull(),
            topSong = db.topSongs(span, 1).firstOrNull(),
            topAlbum = db.topAlbums(span, 1).firstOrNull(),
            favHour = if (hourIdx >= 0) hourIdx else -1,
            favHourMs = if (hourIdx >= 0) hours[hourIdx] else 0L,
            longestDayKey = dayKey,
            longestDayMs = dayMs
        )
    }
}
