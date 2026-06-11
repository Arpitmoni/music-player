package com.musicplayer.data.model

/**
 * Song — a pure data class representing one track from the device library.
 *
 * Every field maps directly to a column returned by MediaStore so we keep
 * this class intentionally simple (no business logic here).
 */
data class Song(
    /** Unique MediaStore row ID — used as ExoPlayer media item ID */
    val id: Long,

    /** Track title (e.g. "Blinding Lights") */
    val title: String,

    /** Artist name (e.g. "The Weeknd") */
    val artist: String,

    /** Album name (e.g. "After Hours") */
    val album: String,

    /** Full file path on disk — used to build the playback URI */
    val path: String,

    /** Track duration in milliseconds */
    val duration: Long,

    /** Album art content URI string (content://media/…) — may be empty */
    val albumArtUri: String
) {
    /** Human-readable duration: mm:ss */
    fun formattedDuration(): String {
        val totalSeconds = duration / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
