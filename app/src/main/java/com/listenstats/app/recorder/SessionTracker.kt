package com.listenstats.app.recorder

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Handler
import android.os.SystemClock
import com.listenstats.app.data.Dates
import com.listenstats.app.data.LiveSession
import com.listenstats.app.data.PlayRow

interface TrackerHost {
    fun minListenMs(): Long
    fun onRow(row: PlayRow)
    fun onTrackerChanged()
    fun onTrackerGone(tracker: SessionTracker)
}

/**
 * Tracks ONE media session. Listening time only accumulates while the state is
 * STATE_PLAYING, measured with SystemClock.elapsedRealtime(). Pauses, buffering,
 * seeking and skipping states do not add time.
 *
 * All methods run on the main thread (the callback handler is the main handler).
 */
class SessionTracker(
    private val controller: MediaController,
    private val handler: Handler,
    private val host: TrackerHost
) : MediaController.Callback() {

    companion object {
        private const val NOT_PLAYING = -1L

        /** A paused session that never resumes is saved after this long. */
        private const val PAUSE_FLUSH_MS = 120_000L

        /**
         * Metadata that changes within this much listening time is treated as the
         * player filling in tags, not as a new track.
         */
        private const val REFINE_MS = 3_000L
    }

    val packageName: String = controller.packageName ?: ""

    var title: String = ""
        private set
    var artist: String = ""
        private set
    private var album: String = ""
    private var durationMs: Long = 0L

    private var accumulatedMs: Long = 0L
    private var playingSince: Long = NOT_PLAYING
    private var startedAt: Long = 0L
    private var disposed: Boolean = false

    private val pauseFlush = Runnable {
        if (!disposed) {
            finishTrack(false)
            host.onTrackerChanged()
        }
    }

    fun attach() {
        controller.registerCallback(this, handler)
        // Seed with what the session is doing right now: metadata first, then state.
        applyMetadata(controller.metadata)
        val st = controller.playbackState
        applyState(st?.state ?: PlaybackState.STATE_NONE)
        host.onTrackerChanged()
    }

    fun dispose() {
        if (disposed) return
        handler.removeCallbacks(pauseFlush)
        try {
            controller.unregisterCallback(this)
        } catch (e: Exception) {
            // already gone
        }
        finishTrack(false)
        disposed = true
    }

    fun toLive(): LiveSession =
        LiveSession(packageName, title, artist, accumulatedMs, playingSince)

    // ------------------------------------------------------------ callbacks

    override fun onPlaybackStateChanged(state: PlaybackState?) {
        if (disposed) return
        applyState(state?.state ?: PlaybackState.STATE_NONE)
        host.onTrackerChanged()
    }

    override fun onMetadataChanged(metadata: MediaMetadata?) {
        if (disposed) return
        applyMetadata(metadata)
        host.onTrackerChanged()
    }

    override fun onSessionDestroyed() {
        if (disposed) return
        host.onTrackerGone(this)
    }

    // ------------------------------------------------------------ logic

    private fun applyState(state: Int) {
        val now = SystemClock.elapsedRealtime()
        when (state) {
            PlaybackState.STATE_PLAYING -> {
                handler.removeCallbacks(pauseFlush)
                if (playingSince == NOT_PLAYING) {
                    playingSince = now
                    if (startedAt == 0L) startedAt = System.currentTimeMillis()
                }
            }
            PlaybackState.STATE_STOPPED,
            PlaybackState.STATE_NONE,
            PlaybackState.STATE_ERROR -> {
                handler.removeCallbacks(pauseFlush)
                finishTrack(false)
            }
            else -> {
                // Paused, buffering, connecting, seeking, skipping: stop the clock.
                stopClock(now)
                handler.removeCallbacks(pauseFlush)
                if (state == PlaybackState.STATE_PAUSED && accumulatedMs > 0L) {
                    handler.postDelayed(pauseFlush, PAUSE_FLUSH_MS)
                }
            }
        }
    }

    private fun applyMetadata(md: MediaMetadata?) {
        var newTitle = md?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        if (newTitle.isBlank()) {
            newTitle = md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE).orEmpty()
        }
        var newArtist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
        if (newArtist.isBlank()) {
            newArtist = md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty()
        }
        val newAlbum = md?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
        val newDuration = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

        val hadTrack = title.isNotBlank()
        val sameTitle = title.equals(newTitle, ignoreCase = true)
        val otherTagsChanged = artist != newArtist || album != newAlbum

        if (hadTrack && !sameTitle) {
            // Real track change: save the old one, keep the clock running for the new one.
            finishTrack(true)
        } else if (hadTrack && sameTitle && otherTagsChanged && currentListenedMs() >= REFINE_MS) {
            // Same title but different artist/album after real listening: a different track.
            finishTrack(true)
        }

        title = newTitle
        artist = newArtist
        album = newAlbum
        durationMs = newDuration
    }

    private fun currentListenedMs(): Long {
        val running = if (playingSince != NOT_PLAYING) {
            SystemClock.elapsedRealtime() - playingSince
        } else {
            0L
        }
        return accumulatedMs + running
    }

    private fun stopClock(now: Long) {
        if (playingSince != NOT_PLAYING) {
            accumulatedMs += now - playingSince
            playingSince = NOT_PLAYING
        }
    }

    /** Saves the current track (if it qualifies) and resets the counters. */
    private fun finishTrack(keepPlaying: Boolean) {
        val now = SystemClock.elapsedRealtime()
        val wasPlaying = playingSince != NOT_PLAYING
        stopClock(now)

        // Copy everything into local vals before handing the row to another thread.
        val listened = accumulatedMs
        val rowTitle = title
        val rowArtist = artist
        val rowAlbum = album
        val rowDuration = durationMs
        val rowStart = startedAt
        val rowPackage = packageName
        val minMs = host.minListenMs().coerceAtLeast(1000L)

        if (listened >= minMs && rowTitle.isNotBlank() && rowArtist.isNotBlank() && rowStart > 0L) {
            host.onRow(
                PlayRow(
                    title = rowTitle,
                    artist = rowArtist,
                    album = rowAlbum,
                    durationMs = rowDuration,
                    listenedMs = listened,
                    timestamp = rowStart,
                    packageName = rowPackage,
                    dayKey = Dates.dayKey(rowStart),
                    monthKey = Dates.monthKey(rowStart),
                    year = Dates.year(rowStart),
                    hourOfDay = Dates.hourOfDay(rowStart)
                )
            )
        }

        accumulatedMs = 0L
        startedAt = 0L
        if (keepPlaying && wasPlaying) {
            playingSince = now
            startedAt = System.currentTimeMillis()
        }
    }
}
