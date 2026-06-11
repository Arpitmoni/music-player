package com.musicplayer.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.musicplayer.data.model.PlayerState
import com.musicplayer.data.model.Song

/**
 * PlayerController
 *
 * A singleton that:
 *  1. Binds to [MusicService]
 *  2. Exposes LiveData for playback state & progress
 *  3. Provides a simple API (play, pause, next, previous, seek)
 *
 * Both [MainActivity] and [NowPlayingActivity] observe the same LiveData
 * so the mini-player and full player are always in sync.
 *
 * Call [bind] in Activity.onStart() and [unbind] in Activity.onStop().
 * Because multiple activities share this singleton the service stays bound
 * as long as at least one activity is visible.
 */
object PlayerController {

    private var service: MusicService? = null
    private var bindCount = 0   // track how many components have bound

    // ── Exposed state ─────────────────────────────────────────────────────
    private val _playerState = MutableLiveData<PlayerState>(PlayerState.Idle)
    val playerState: LiveData<PlayerState> get() = _playerState

    private val _progress = MutableLiveData<Pair<Long, Long>>(0L to 0L) // position to duration
    val progress: LiveData<Pair<Long, Long>> get() = _progress

    // ── Service connection ────────────────────────────────────────────────
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val musicBinder = binder as MusicService.MusicBinder
            service = musicBinder.getService().also { svc ->

                // Wire service callbacks → LiveData
                svc.onPlaybackStateChanged = { isPlaying, song ->
                    song?.let {
                        val pos = svc.player.currentPosition
                        val dur = svc.player.duration.coerceAtLeast(0)
                        _playerState.postValue(
                            if (isPlaying) PlayerState.Playing(it, pos, dur)
                            else           PlayerState.Paused(it, pos, dur)
                        )
                    } ?: run {
                        _playerState.postValue(PlayerState.Idle)
                    }
                }

                svc.onProgressChanged = { pos, dur ->
                    _progress.postValue(pos to dur)
                    // Keep playerState position in sync
                    val current = _playerState.value
                    if (current is PlayerState.Playing) {
                        _playerState.postValue(current.copy(currentPosition = pos, duration = dur))
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Lifecycle hooks — call from Activity.onStart / onStop
    // ─────────────────────────────────────────────────────────────────────

    fun bind(context: Context) {
        bindCount++
        val intent = Intent(context, MusicService::class.java)
        context.startService(intent)  // ensure service survives unbind
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbind(context: Context) {
        bindCount--
        if (bindCount <= 0) {
            bindCount = 0
            try { context.unbindService(connection) } catch (_: Exception) {}
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Playback API
    // ─────────────────────────────────────────────────────────────────────

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) {
        service?.playPlaylist(songs, startIndex)
    }

    fun playSong(song: Song) {
        service?.playSong(song)
    }

    fun togglePlayPause() {
        service?.togglePlayPause()
    }

    fun skipToNext() {
        service?.skipToNext()
    }

    fun skipToPrevious() {
        service?.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        service?.seekTo(positionMs)
    }

    val isPlaying: Boolean get() = service?.player?.isPlaying == true
    fun currentPosition(): Long = service?.player?.currentPosition ?: 0L
    fun duration(): Long = service?.player?.duration?.coerceAtLeast(0) ?: 0L
}
