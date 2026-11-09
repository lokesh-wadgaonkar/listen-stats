package com.listenstats.app.ui

import com.listenstats.app.data.Dates
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Fmt {

    /** "3h 12m", "45m" or "30s". */
    fun duration(ms: Long): String {
        val totalMin = ms / 60_000L
        val h = totalMin / 60L
        val m = totalMin % 60L
        return when {
            h > 0L -> "${h}h ${m}m"
            totalMin > 0L -> "${m}m"
            else -> "${ms / 1000L}s"
        }
    }

    fun minutes(ms: Long): String = (ms / 60_000L).toString()

    /** "m:ss" */
    fun clock(ms: Long): String {
        val total = ms / 1000L
        val m = total / 60L
        val sec = total % 60L
        return m.toString() + ":" + (if (sec < 10L) "0" else "") + sec.toString()
    }

    fun plays(n: Int): String = if (n == 1) "1 play" else "$n plays"

    fun hourLabel(h: Int): String = when {
        h == 0 -> "12 AM"
        h < 12 -> "$h AM"
        h == 12 -> "12 PM"
        else -> "${h - 12} PM"
    }

    private fun dateOfKey(dayKey: Int): Date {
        val y = dayKey / 10000
        val m = (dayKey / 100) % 100
        val d = dayKey % 100
        val c = Calendar.getInstance()
        c.clear()
        c.set(y, m - 1, d, 12, 0, 0)
        return c.time
    }

    fun dayHeader(dayKey: Int): String {
        if (dayKey == Dates.todayKey()) return "Today"
        if (dayKey == Dates.yesterdayKey()) return "Yesterday"
        return SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(dateOfKey(dayKey))
    }

    fun dayLong(dayKey: Int): String =
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(dateOfKey(dayKey))

    fun dayShort(dayKey: Int): String =
        SimpleDateFormat("d MMM", Locale.getDefault()).format(dateOfKey(dayKey))

    fun timeOfDay(ms: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))

    fun monthLong(monthKey: Int): String {
        val c = Calendar.getInstance()
        c.clear()
        c.set(monthKey / 100, (monthKey % 100) - 1, 1, 12, 0, 0)
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(c.time)
    }

    fun monthShort(monthKey: Int): String {
        val c = Calendar.getInstance()
        c.clear()
        c.set(monthKey / 100, (monthKey % 100) - 1, 1, 12, 0, 0)
        return SimpleDateFormat("MMM", Locale.getDefault()).format(c.time)
    }
}
