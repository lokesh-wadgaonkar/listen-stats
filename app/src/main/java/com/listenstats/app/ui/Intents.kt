package com.listenstats.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object Intents {

    fun openNotificationAccess(ctx: Context) {
        start(ctx, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS), null)
    }

    fun openAppInfo(ctx: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + ctx.packageName)
        )
        start(ctx, intent, null)
    }

    /** The app's battery settings: the app-info page, where Battery usage lives on ColorOS. */
    fun openBatterySettings(ctx: Context) {
        openAppInfo(ctx)
    }

    /** System list of battery-optimization exemptions (falls back to app info). */
    fun openBatteryOptimizationList(ctx: Context) {
        start(ctx, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATIONS_SETTINGS)) {
            openAppInfo(ctx)
        }
    }

    private fun start(ctx: Context, intent: Intent, fallback: (() -> Unit)?) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(intent)
        } catch (e: Exception) {
            if (fallback != null) {
                fallback()
            }
        }
    }
}
