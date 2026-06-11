package com.musicplayer.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.musicplayer.service.PlayerController

/**
 * NowPlayingViewModel
 *
 * Owned by NowPlayingActivity.  Thin wrapper — player state is entirely
 * driven by [PlayerController] which is shared with [MainViewModel].
 * This keeps the single source of truth in the service layer.
 */
class NowPlayingViewModel : ViewModel() {

    val playerState: LiveData<com.musicplayer.data.model.PlayerState> = PlayerController.playerState
    val progress:    LiveData<Pair<Long, Long>>                       = PlayerController.progress

    fun togglePlayPause() = PlayerController.togglePlayPause()
    fun skipToNext()      = PlayerController.skipToNext()
    fun skipToPrevious()  = PlayerController.skipToPrevious()
    fun seekTo(ms: Long)  = PlayerController.seekTo(ms)
}
