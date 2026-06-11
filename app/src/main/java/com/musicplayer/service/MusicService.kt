package com.musicplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.musicplayer.R
import com.musicplayer.data.model.Song
import com.musicplayer.ui.home.MainActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * MusicService
 *
 * A foreground service that wraps ExoPlayer.  By running as a foreground
 * service we can continue playback when the user leaves the app, and we
 * display a persistent media notification with transport controls.
 *
 * The service uses a Binder so that Activities/Fragments can call player
 * methods directly without going through broadcast intents.
 *
 * Architecture note: the service does NOT hold ViewModel state.  It just
 * plays audio.  The ViewModel observes a shared PlayerController singleton
 * that bridges the service and the UI layer.
 */
class MusicService : LifecycleService() {

    // ── Binder ────────────────────────────────────────────────────────────
    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    private val binder = MusicBinder()

    // ── ExoPlayer ─────────────────────────────────────────────────────────
    lateinit var player: ExoPlayer
        private set

    // ── Current playlist ──────────────────────────────────────────────────
    private var playlist: List<Song> = emptyList()
    var currentIndex: Int = -1
        private set

    // ── Progress polling job ──────────────────────────────────────────────
    private var progressJob: Job? = null

    // ── Callback interface (observed by ViewModel via PlayerController) ───
    var onPlaybackStateChanged: ((isPlaying: Boolean, song: Song?) -> Unit)? = null
    var onProgressChanged: ((position: Long, duration: Long) -> Unit)? = null

    // ── Notification constants ────────────────────────────────────────────
    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "music_playback_channel"

        // Intent actions for notification buttons
        const val ACTION_PLAY_PAUSE = "com.musicplayer.PLAY_PAUSE"
        const val ACTION_NEXT       = "com.musicplayer.NEXT"
        const val ACTION_PREV       = "com.musicplayer.PREV"
        const val ACTION_STOP       = "com.musicplayer.STOP"
    }

    // ─────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initPlayer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Handle notification button actions
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_NEXT       -> skipToNext()
            ACTION_PREV       -> skipToPrevious()
            ACTION_STOP       -> { stopSelf(); return START_NOT_STICKY }
        }

        return START_STICKY // Restart if killed by system
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onDestroy() {
        progressJob?.cancel()
        player.release()
        super.onDestroy()
    }

    // ─────────────────────────────────────────────────────────────────────
    // Player initialisation
    // ─────────────────────────────────────────────────────────────────────

    private fun initPlayer() {
        player = ExoPlayer.Builder(this).build().also { exo ->
            exo.addListener(object : Player.Listener {

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            notifyStateChanged()
                            updateNotification()
                            startProgressPolling()
                        }
                        Player.STATE_ENDED -> skipToNext()
                        else -> {}
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    notifyStateChanged()
                    updateNotification()
                    if (isPlaying) startProgressPolling() else progressJob?.cancel()
                }
            })
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Public playback controls (called by ViewModel via bound service)
    // ─────────────────────────────────────────────────────────────────────

    /** Load a new playlist and immediately start playing [startIndex]. */
    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) {
        playlist = songs
        currentIndex = startIndex.coerceIn(0, songs.lastIndex)
        loadAndPlay(playlist[currentIndex])
    }

    /** Play a single song (clears any existing playlist). */
    fun playSong(song: Song) {
        val idx = playlist.indexOf(song)
        if (idx >= 0) {
            currentIndex = idx
            loadAndPlay(song)
        } else {
            playlist = listOf(song)
            currentIndex = 0
            loadAndPlay(song)
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun skipToNext() {
        if (playlist.isEmpty()) return
        currentIndex = (currentIndex + 1) % playlist.size
        loadAndPlay(playlist[currentIndex])
    }

    fun skipToPrevious() {
        if (playlist.isEmpty()) return
        // If we're more than 3 seconds in, restart current track
        if (player.currentPosition > 3000) {
            player.seekTo(0)
            return
        }
        currentIndex = if (currentIndex > 0) currentIndex - 1 else playlist.lastIndex
        loadAndPlay(playlist[currentIndex])
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun currentSong(): Song? = playlist.getOrNull(currentIndex)

    // ─────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────

    private fun loadAndPlay(song: Song) {
        val mediaItem = MediaItem.fromUri(song.path)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        startForeground(NOTIFICATION_ID, buildNotification(song))
    }

    private fun notifyStateChanged() {
        onPlaybackStateChanged?.invoke(player.isPlaying, currentSong())
    }

    /** Poll playback position every 500 ms and emit to listeners. */
    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = lifecycleScope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    onProgressChanged?.invoke(
                        player.currentPosition,
                        player.duration.coerceAtLeast(0)
                    )
                }
                delay(500)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Notification
    // ─────────────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW  // Silent — no sound/vibration
            ).apply {
                description = "Shows currently playing track"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(song: Song): Notification {
        // Tap notification → open app
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun actionIntent(action: String, reqCode: Int) =
            PendingIntent.getService(
                this, reqCode,
                Intent(this, MusicService::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val playPauseIcon = if (player.isPlaying) R.drawable.ic_pause else R.drawable.ic_play

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(openAppIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(player.isPlaying)
            // Transport controls
            .addAction(R.drawable.ic_previous, "Previous", actionIntent(ACTION_PREV, 1))
            .addAction(playPauseIcon,          "Play/Pause", actionIntent(ACTION_PLAY_PAUSE, 2))
            .addAction(R.drawable.ic_next,     "Next", actionIntent(ACTION_NEXT, 3))
            .addAction(R.drawable.ic_close,    "Stop", actionIntent(ACTION_STOP, 4))
            // Show as media-style notification (lock screen art, etc.)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun updateNotification() {
        val song = currentSong() ?: return
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(song))
    }
}
