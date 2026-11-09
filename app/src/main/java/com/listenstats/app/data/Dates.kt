package com.listenstats.app.data

import java.util.Calendar

/**
 * Date helpers. Keys are built from Calendar fields (year*10000 + month*100 + day),
 * never from DateTimeFormatter.BASIC_ISO_DATE.
 */
object Dates {

    fun dayKey(ms: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        return dayKeyOf(c)
    }

    fun dayKeyOf(c: Calendar): Int {
        return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
    }

    fun monthKey(ms: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        return c.get(Calendar.YEAR) * 100 + (c.get(Calendar.MONTH) + 1)
    }

    fun year(ms: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        return c.get(Calendar.YEAR)
    }

    fun hourOfDay(ms: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        return c.get(Calendar.HOUR_OF_DAY)
    }

    fun todayKey(): Int = dayKey(System.currentTimeMillis())

    fun yesterdayKey(): Int {
        val c = startOfDay(System.currentTimeMillis())
        c.add(Calendar.DAY_OF_YEAR, -1)
        return dayKeyOf(c)
    }

    fun startOfDay(ms: Long): Calendar {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c
    }

    fun spanFor(range: StatRange, nowMs: Long = System.currentTimeMillis()): TimeSpan {
        return when (range) {
            StatRange.TODAY -> {
                val start = startOfDay(nowMs)
                val end = startOfDay(nowMs)
                end.add(Calendar.DAY_OF_YEAR, 1)
                TimeSpan(start.timeInMillis, end.timeInMillis)
            }
            StatRange.WEEK -> {
                val start = startOfDay(nowMs)
                start.add(Calendar.DAY_OF_YEAR, -6)
                val end = startOfDay(nowMs)
                end.add(Calendar.DAY_OF_YEAR, 1)
                TimeSpan(start.timeInMillis, end.timeInMillis)
            }
            StatRange.MONTH -> {
                val start = startOfDay(nowMs)
                start.set(Calendar.DAY_OF_MONTH, 1)
                val end = startOfDay(nowMs)
                end.set(Calendar.DAY_OF_MONTH, 1)
                end.add(Calendar.MONTH, 1)
                TimeSpan(start.timeInMillis, end.timeInMillis)
            }
            StatRange.YEAR -> {
                val start = startOfDay(nowMs)
                start.set(Calendar.MONTH, Calendar.JANUARY)
                start.set(Calendar.DAY_OF_MONTH, 1)
                val end = startOfDay(nowMs)
                end.set(Calendar.MONTH, Calendar.JANUARY)
                end.set(Calendar.DAY_OF_MONTH, 1)
                end.add(Calendar.YEAR, 1)
                TimeSpan(start.timeInMillis, end.timeInMillis)
            }
            StatRange.ALL -> TimeSpan(0L, Long.MAX_VALUE)
        }
    }

    /** month1 is 1..12. */
    fun monthSpan(year: Int, month1: Int): TimeSpan {
        val c = Calendar.getInstance()
        c.clear()
        c.set(year, month1 - 1, 1, 0, 0, 0)
        val from = c.timeInMillis
        c.add(Calendar.MONTH, 1)
        return TimeSpan(from, c.timeInMillis)
    }

    fun yearSpan(year: Int): TimeSpan {
        val c = Calendar.getInstance()
        c.clear()
        c.set(year, Calendar.JANUARY, 1, 0, 0, 0)
        val from = c.timeInMillis
        c.add(Calendar.YEAR, 1)
        return TimeSpan(from, c.timeInMillis)
    }
}
