package com.musicplayer.data.model

/**
 * PlayerState — describes every possible state of the music player UI.
 *
 * The ViewModel exposes a LiveData<PlayerState> so that both the
 * Home screen and the Now Playing screen always reflect the same truth.
 */
sealed class PlayerState {

    /** Nothing is playing yet (app just launched) */
    object Idle : PlayerState()

    /** ExoPlayer is buffering / preparing the track */
    object Loading : PlayerState()

    /** A track is actively playing */
    data class Playing(
        val song: Song,
        val currentPosition: Long = 0L,
        val duration: Long = 0L
    ) : PlayerState()

    /** A track is paused */
    data class Paused(
        val song: Song,
        val currentPosition: Long = 0L,
        val duration: Long = 0L
    ) : PlayerState()

    /** An error occurred (e.g. file not found) */
    data class Error(val message: String) : PlayerState()
}

/** Convenience extension — true when a song is loaded (playing or paused) */
val PlayerState.hasSong: Boolean
    get() = this is PlayerState.Playing || this is PlayerState.Paused

/** Convenience extension — extract the current song regardless of play/pause */
val PlayerState.currentSong: Song?
    get() = when (this) {
        is PlayerState.Playing -> song
        is PlayerState.Paused  -> song
        else                   -> null
    }

/** Convenience extension — is music currently playing? */
val PlayerState.isPlaying: Boolean
    get() = this is PlayerState.Playing
