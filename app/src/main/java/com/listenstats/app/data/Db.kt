package com.listenstats.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

/** Plain SQLiteOpenHelper storage. Call from a background thread. */
class Db private constructor(context: Context) :
    SQLiteOpenHelper(context, "listenstats.db", null, 1) {

    companion object {
        /** Bumped after every write so the UI knows to reload. */
        val version = MutableStateFlow(0)

        @Volatile
        private var instance: Db? = null

        fun get(context: Context): Db {
            val existing = instance
            if (existing != null) return existing
            return synchronized(this) {
                val again = instance
                if (again != null) {
                    again
                } else {
                    val created = Db(context.applicationContext)
                    instance = created
                    created
                }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE plays (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "artist TEXT NOT NULL, " +
                "album TEXT NOT NULL, " +
                "durationMs INTEGER NOT NULL, " +
                "listenedMs INTEGER NOT NULL, " +
                "timestamp INTEGER NOT NULL, " +
                "packageName TEXT NOT NULL, " +
                "dayKey INTEGER NOT NULL, " +
                "monthKey INTEGER NOT NULL, " +
                "year INTEGER NOT NULL, " +
                "hourOfDay INTEGER NOT NULL)"
        )
        db.execSQL("CREATE UNIQUE INDEX idx_plays_unique ON plays(timestamp, packageName, title, artist)")
        db.execSQL("CREATE INDEX idx_plays_ts ON plays(timestamp)")
        db.execSQL(
            "CREATE TABLE apps (" +
                "packageName TEXT PRIMARY KEY, " +
                "label TEXT NOT NULL, " +
                "enabled INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 only.
    }

    private fun w(span: TimeSpan): String =
        "timestamp >= ${span.from} AND timestamp < ${span.to}"

    // ---------------------------------------------------------------- writes

    fun insertPlay(r: PlayRow) {
        val cv = ContentValues()
        cv.put("title", r.title)
        cv.put("artist", r.artist)
        cv.put("album", r.album)
        cv.put("durationMs", r.durationMs)
        cv.put("listenedMs", r.listenedMs)
        cv.put("timestamp", r.timestamp)
        cv.put("packageName", r.packageName)
        cv.put("dayKey", r.dayKey)
        cv.put("monthKey", r.monthKey)
        cv.put("year", r.year)
        cv.put("hourOfDay", r.hourOfDay)
        writableDatabase.insertWithOnConflict("plays", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        version.update { it + 1 }
    }

    fun clearPlays() {
        writableDatabase.delete("plays", null, null)
        version.update { it + 1 }
    }

    // ---------------------------------------------------------------- stats

    fun totals(span: TimeSpan): Totals {
        val sql = "SELECT COALESCE(SUM(listenedMs), 0), COUNT(*) FROM plays WHERE ${w(span)}"
        return readableDatabase.rawQuery(sql, null).use { c ->
            if (c.moveToFirst()) Totals(c.getLong(0), c.getInt(1)) else Totals(0L, 0)
        }
    }

    private fun rank(sql: String): List<RankItem> {
        val out = ArrayList<RankItem>()
        readableDatabase.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) {
                out.add(RankItem(c.getString(0) ?: "", c.getString(1) ?: "", c.getLong(2), c.getInt(3)))
            }
        }
        return out
    }

    fun topArtists(span: TimeSpan, limit: Int): List<RankItem> = rank(
        "SELECT artist, '' AS sub, SUM(listenedMs) AS total, COUNT(*) AS cnt FROM plays " +
            "WHERE ${w(span)} GROUP BY artist ORDER BY total DESC LIMIT $limit"
    )

    fun topSongs(span: TimeSpan, limit: Int): List<RankItem> = rank(
        "SELECT title, artist AS sub, SUM(listenedMs) AS total, COUNT(*) AS cnt FROM plays " +
            "WHERE ${w(span)} GROUP BY title, artist ORDER BY total DESC LIMIT $limit"
    )

    fun topAlbums(span: TimeSpan, limit: Int): List<RankItem> = rank(
        "SELECT album, MIN(artist) AS sub, SUM(listenedMs) AS total, COUNT(*) AS cnt FROM plays " +
            "WHERE ${w(span)} AND album <> '' GROUP BY album ORDER BY total DESC LIMIT $limit"
    )

    /** 24 values, index = hour of day. */
    fun byHour(span: TimeSpan): List<Long> {
        val out = LongArray(24)
        val sql = "SELECT hourOfDay, SUM(listenedMs) FROM plays WHERE ${w(span)} GROUP BY hourOfDay"
        readableDatabase.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) {
                val h = c.getInt(0)
                if (h in 0..23) out[h] = c.getLong(1)
            }
        }
        return out.toList()
    }

    /** dayKey (yyyymmdd) to listened ms. */
    fun byDay(span: TimeSpan): Map<Int, Long> {
        val out = HashMap<Int, Long>()
        val sql = "SELECT dayKey, SUM(listenedMs) FROM plays WHERE ${w(span)} GROUP BY dayKey"
        readableDatabase.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) {
                out[c.getInt(0)] = c.getLong(1)
            }
        }
        return out
    }

    /** monthKey (yyyymm) to listened ms. */
    fun byMonth(span: TimeSpan): Map<Int, Long> {
        val out = HashMap<Int, Long>()
        val sql = "SELECT monthKey, SUM(listenedMs) FROM plays WHERE ${w(span)} GROUP BY monthKey"
        readableDatabase.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) {
                out[c.getInt(0)] = c.getLong(1)
            }
        }
        return out
    }

    fun history(limit: Int): List<HistoryItem> {
        val out = ArrayList<HistoryItem>()
        val sql = "SELECT title, artist, album, listenedMs, timestamp, packageName, dayKey " +
            "FROM plays ORDER BY timestamp DESC LIMIT $limit"
        readableDatabase.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) {
                out.add(
                    HistoryItem(
                        c.getString(0) ?: "",
                        c.getString(1) ?: "",
                        c.getString(2) ?: "",
                        c.getLong(3),
                        c.getLong(4),
                        c.getString(5) ?: "",
                        c.getInt(6)
                    )
                )
            }
        }
        return out
    }

    fun distinctMonths(): List<Int> {
        val out = ArrayList<Int>()
        readableDatabase.rawQuery("SELECT DISTINCT monthKey FROM plays ORDER BY monthKey DESC", null).use { c ->
            while (c.moveToNext()) out.add(c.getInt(0))
        }
        return out
    }

    fun distinctYears(): List<Int> {
        val out = ArrayList<Int>()
        readableDatabase.rawQuery("SELECT DISTINCT year FROM plays ORDER BY year DESC", null).use { c ->
            while (c.moveToNext()) out.add(c.getInt(0))
        }
        return out
    }

    // ---------------------------------------------------------------- apps

    fun upsertApp(pkg: String, label: String, defaultEnabled: Boolean) {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put("packageName", pkg)
        cv.put("label", label)
        cv.put("enabled", if (defaultEnabled) 1 else 0)
        db.insertWithOnConflict("apps", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        val up = ContentValues()
        up.put("label", label)
        db.update("apps", up, "packageName = ?", arrayOf(pkg))
        version.update { it + 1 }
    }

    fun setEnabled(pkg: String, enabled: Boolean) {
        val cv = ContentValues()
        cv.put("enabled", if (enabled) 1 else 0)
        writableDatabase.update("apps", cv, "packageName = ?", arrayOf(pkg))
        version.update { it + 1 }
    }

    fun isEnabled(pkg: String): Boolean {
        return readableDatabase.rawQuery(
            "SELECT enabled FROM apps WHERE packageName = ?", arrayOf(pkg)
        ).use { c ->
            c.moveToFirst() && c.getInt(0) == 1
        }
    }

    fun apps(): List<AppEntry> {
        val out = ArrayList<AppEntry>()
        readableDatabase.rawQuery(
            "SELECT packageName, label, enabled FROM apps ORDER BY label COLLATE NOCASE ASC", null
        ).use { c ->
            while (c.moveToNext()) {
                out.add(AppEntry(c.getString(0) ?: "", c.getString(1) ?: "", c.getInt(2) == 1))
            }
        }
        return out
    }

    // ---------------------------------------------------------------- backup

    fun exportJson(): String {
        val root = JSONObject()
        root.put("app", "ListenStats")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val plays = JSONArray()
        val sql = "SELECT title, artist, album, durationMs, listenedMs, timestamp, packageName " +
            "FROM plays ORDER BY timestamp ASC"
        readableDatabase.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) {
                val o = JSONObject()
                o.put("title", c.getString(0) ?: "")
                o.put("artist", c.getString(1) ?: "")
                o.put("album", c.getString(2) ?: "")
                o.put("durationMs", c.getLong(3))
                o.put("listenedMs", c.getLong(4))
                o.put("timestamp", c.getLong(5))
                o.put("packageName", c.getString(6) ?: "")
                plays.put(o)
            }
        }
        root.put("plays", plays)

        val appsArr = JSONArray()
        for (a in apps()) {
            val o = JSONObject()
            o.put("packageName", a.packageName)
            o.put("label", a.label)
            o.put("enabled", a.enabled)
            appsArr.put(o)
        }
        root.put("apps", appsArr)
        return root.toString()
    }

    /** Returns the number of plays that were actually added (duplicates are skipped). */
    fun importJson(text: String): Int {
        val root = JSONObject(text)
        val plays = root.optJSONArray("plays")
            ?: throw IllegalArgumentException("This file has no plays list.")
        val db = writableDatabase
        var added = 0
        db.beginTransaction()
        try {
            for (i in 0 until plays.length()) {
                val o = plays.getJSONObject(i)
                val title = o.optString("title", "")
                if (title.isBlank()) continue
                val ts = o.getLong("timestamp")
                val cv = ContentValues()
                cv.put("title", title)
                cv.put("artist", o.optString("artist", ""))
                cv.put("album", o.optString("album", ""))
                cv.put("durationMs", o.optLong("durationMs", 0L))
                cv.put("listenedMs", o.optLong("listenedMs", 0L))
                cv.put("timestamp", ts)
                cv.put("packageName", o.optString("packageName", ""))
                cv.put("dayKey", Dates.dayKey(ts))
                cv.put("monthKey", Dates.monthKey(ts))
                cv.put("year", Dates.year(ts))
                cv.put("hourOfDay", Dates.hourOfDay(ts))
                val id = db.insertWithOnConflict("plays", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
                if (id != -1L) added++
            }
            val appsArr = root.optJSONArray("apps")
            if (appsArr != null) {
                for (i in 0 until appsArr.length()) {
                    val o = appsArr.getJSONObject(i)
                    val pkg = o.optString("packageName", "")
                    if (pkg.isBlank()) continue
                    val cv = ContentValues()
                    cv.put("packageName", pkg)
                    cv.put("label", o.optString("label", pkg))
                    cv.put("enabled", if (o.optBoolean("enabled", false)) 1 else 0)
                    db.insertWithOnConflict("apps", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        version.update { it + 1 }
        return added
    }
}
