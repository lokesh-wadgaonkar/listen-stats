package com.listenstats.app.data

import android.content.Context

class Prefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("listenstats_prefs", Context.MODE_PRIVATE)

    var minSeconds: Int
        get() = sp.getInt("min_seconds", 30)
        set(value) {
            sp.edit().putInt("min_seconds", value).apply()
        }

    var amoled: Boolean
        get() = sp.getBoolean("amoled", false)
        set(value) {
            sp.edit().putBoolean("amoled", value).apply()
        }

    var dynamicColor: Boolean
        get() = sp.getBoolean("dynamic_color", true)
        set(value) {
            sp.edit().putBoolean("dynamic_color", value).apply()
        }
}
