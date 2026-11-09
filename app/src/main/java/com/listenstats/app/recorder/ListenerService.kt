package com.listenstats.app.recorder

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import com.listenstats.app.data.Db
import com.listenstats.app.data.PlayRow
import com.listenstats.app.data.Prefs
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Hosts the recorder. Being an enabled NotificationListenerService is what allows
 * MediaSessionManager.getActiveSessions() to work for this app.
 */
class ListenerService : NotificationListenerService(), TrackerHost {

    companion object {
        const val POWERAMP = "com.maxmpz.audioplayer"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val io: ExecutorService = Executors.newSingleThreadExecutor()
    private val trackers = HashMap<MediaSession.Token, SessionTracker>()

    private val db: Db by lazy { Db.get(applicationContext) }
    private val prefs: Prefs by lazy { Prefs(applicationContext) }

    private var manager: MediaSessionManager? = null
    private var listening = false

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            syncSessions(controllers)
        }

    // ------------------------------------------------------------ service lifecycle

    override fun onListenerConnected() {
        super.onListenerConnected()
        startListening()
    }

    override fun onListenerDisconnected() {
        stopListening()
        super.onListenerDisconnected()
        try {
            NotificationListenerService.requestRebind(
                ComponentName(this, ListenerService::class.java)
            )
        } catch (e: Exception) {
            // not allowed right now; the system or the app will rebind later
        }
    }

    override fun onDestroy() {
        stopListening()
        io.shutdown()
        super.onDestroy()
    }

    // ------------------------------------------------------------ sessions

    private fun startListening() {
        if (listening) return
        val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
        val component = ComponentName(this, ListenerService::class.java)
        try {
            msm.addOnActiveSessionsChangedListener(sessionsListener, component, mainHandler)
            manager = msm
            listening = true
            LiveState.connected.value = true
            syncSessions(msm.getActiveSessions(component))
        } catch (e: SecurityException) {
            // Notification access is not actually granted.
            listening = false
            LiveState.connected.value = false
        }
    }

    private fun stopListening() {
        if (listening) {
            try {
                manager?.removeOnActiveSessionsChangedListener(sessionsListener)
            } catch (e: Exception) {
                // ignore
            }
        }
        listening = false
        val all = trackers.values.toList()
        trackers.clear()
        for (t in all) {
            t.dispose()
        }
        LiveState.sessions.value = emptyList()
        LiveState.connected.value = false
    }

    private fun syncSessions(list: List<MediaController>?) {
        val current: List<MediaController> = list ?: emptyList()

        val liveTokens = HashSet<MediaSession.Token>()
        for (c in current) {
            liveTokens.add(c.sessionToken)
        }

        // Sessions that disappeared: flush, unregister, forget.
        val gone = trackers.keys.filter { !liveTokens.contains(it) }
        for (token in gone) {
            val t = trackers.remove(token)
            t?.dispose()
        }

        // New sessions: register a callback and seed it.
        for (c in current) {
            val token = c.sessionToken
            if (!trackers.containsKey(token)) {
                val t = SessionTracker(c, mainHandler, this)
                trackers[token] = t
                registerApp(t.packageName)
                t.attach()
            }
        }
        publishLive()
    }

    private fun registerApp(pkg: String) {
        if (pkg.isBlank()) return
        val defaultOn = pkg == POWERAMP
        io.execute {
            val label = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
            } catch (e: Exception) {
                pkg
            }
            db.upsertApp(pkg, label, defaultOn)
        }
    }

    private fun publishLive() {
        LiveState.sessions.value = trackers.values
            .filter { it.title.isNotBlank() }
            .map { it.toLive() }
    }

    // ------------------------------------------------------------ TrackerHost

    override fun minListenMs(): Long = prefs.minSeconds * 1000L

    override fun onRow(row: PlayRow) {
        try {
            io.execute {
                if (db.isEnabled(row.packageName)) {
                    db.insertPlay(row)
                }
            }
        } catch (e: Exception) {
            // executor already shut down
        }
    }

    override fun onTrackerChanged() {
        publishLive()
    }

    override fun onTrackerGone(tracker: SessionTracker) {
        val iter = trackers.entries.iterator()
        while (iter.hasNext()) {
            if (iter.next().value === tracker) {
                iter.remove()
            }
        }
        tracker.dispose()
        publishLive()
    }
}
