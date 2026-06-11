package com.musicplayer.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.musicplayer.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SongRepository
 *
 * Single source of truth for music data.  Queries Android's MediaStore
 * content provider on an IO coroutine and returns a list of [Song] objects.
 *
 * This class has no Android lifecycle dependency — it only needs a Context
 * to call ContentResolver, which makes it easy to unit-test with a mock context.
 */
class SongRepository(private val context: Context) {

    /**
     * Load all audio files from device storage.
     *
     * Runs on [Dispatchers.IO] so it's safe to call from a ViewModel's
     * viewModelScope without blocking the main thread.
     *
     * @return Sorted list of [Song] objects (sorted A→Z by title).
     */
    suspend fun getAllSongs(): List<Song> = withContext(Dispatchers.IO) {

        val songs = mutableListOf<Song>()

        // ── Columns we want back from MediaStore ──────────────────────────
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,          // file path
            MediaStore.Audio.Media.DURATION
        )

        // ── Only return actual music (not ringtones / notifications) ──────
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 " +
                "AND ${MediaStore.Audio.Media.DURATION} > 30000"  // > 30 seconds

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        // Query MediaStore on IO thread
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->

            // Get column indices once (faster than repeated getColumnIndex calls)
            val idCol       = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id      = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)

                // Build a content URI for album art (works without external storage)
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                songs.add(
                    Song(
                        id          = id,
                        title       = cursor.getString(titleCol)   ?: "Unknown",
                        artist      = cursor.getString(artistCol)  ?: "Unknown Artist",
                        album       = cursor.getString(albumCol)   ?: "Unknown Album",
                        path        = cursor.getString(dataCol)    ?: "",
                        duration    = cursor.getLong(durationCol),
                        albumArtUri = albumArtUri
                    )
                )
            }
        }

        songs
    }
}
