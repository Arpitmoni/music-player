package com.musicplayer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.musicplayer.data.model.Song
import com.musicplayer.data.repository.SongRepository
import com.musicplayer.service.PlayerController
import kotlinx.coroutines.launch

/**
 * MainViewModel
 *
 * Owned by MainActivity. Responsibilities:
 *  - Load songs from [SongRepository] on a background coroutine.
 *  - Expose the loaded list + loading/error states to the UI via LiveData.
 *  - Forward play commands to [PlayerController].
 *
 * The ViewModel survives configuration changes (screen rotation) so the
 * song list doesn't reload every time.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SongRepository(application)

    // ── Song list ─────────────────────────────────────────────────────────
    private val _songs = MutableLiveData<List<Song>>()
    val songs: LiveData<List<Song>> get() = _songs

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    // ── Player state (delegated to PlayerController) ──────────────────────
    val playerState = PlayerController.playerState
    val progress    = PlayerController.progress

    // ─────────────────────────────────────────────────────────────────────
    // Load songs
    // ─────────────────────────────────────────────────────────────────────

    fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = repository.getAllSongs()
                _songs.value = result

                if (result.isEmpty()) {
                    _error.value = "No music found on this device"
                }
            } catch (e: Exception) {
                _error.value = "Failed to load music: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Playback commands
    // ─────────────────────────────────────────────────────────────────────

    fun playSong(index: Int) {
        val songList = _songs.value ?: return
        PlayerController.playPlaylist(songList, index)
    }

    fun togglePlayPause() = PlayerController.togglePlayPause()
    fun skipToNext()      = PlayerController.skipToNext()
    fun skipToPrevious()  = PlayerController.skipToPrevious()
}
