package com.musicplayer.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.musicplayer.R
import com.musicplayer.data.model.PlayerState
import com.musicplayer.data.model.currentSong
import com.musicplayer.data.model.hasSong
import com.musicplayer.data.model.isPlaying
import com.musicplayer.databinding.ActivityMainBinding
import com.musicplayer.service.PlayerController
import com.musicplayer.ui.adapter.SongAdapter
import com.musicplayer.ui.player.NowPlayingActivity
import com.musicplayer.ui.viewmodel.MainViewModel
import com.musicplayer.utils.animateFadeIn
import com.musicplayer.utils.animateSlideUp

/**
 * MainActivity
 *
 * The app's entry point.  Displays:
 *  - A header with app name and track count
 *  - A RecyclerView of all device songs
 *  - A bottom mini-player that slides up when playback starts
 *
 * Permissions are requested at runtime (READ_MEDIA_AUDIO / READ_EXTERNAL_STORAGE).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: SongAdapter

    // ── Permission launcher ───────────────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.loadSongs()
        else showPermissionDenied()
    }

    // ─────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeViewModel()
        checkAndRequestPermission()
        setupMiniPlayer()
    }

    override fun onStart() {
        super.onStart()
        PlayerController.bind(this)
    }

    override fun onStop() {
        super.onStop()
        PlayerController.unbind(this)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = SongAdapter { song, index ->
            viewModel.playSong(index)
        }

        binding.rvSongs.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            // Smooth scroll when items animate in
            itemAnimator?.changeDuration = 200
        }
    }

    private fun setupMiniPlayer() {
        binding.miniPlayer.root.setOnClickListener {
            openNowPlaying()
        }
        binding.miniPlayer.btnMiniPlayPause.setOnClickListener {
            viewModel.togglePlayPause()
        }
        binding.miniPlayer.btnMiniNext.setOnClickListener {
            viewModel.skipToNext()
        }
    }

    private fun observeViewModel() {

        // ── Song list ─────────────────────────────────────────────────────
        viewModel.songs.observe(this) { songs ->
            adapter.submitList(songs)
            binding.tvTrackCount.text = "${songs.size} songs"
            binding.rvSongs.animateFadeIn()
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = it
            } ?: run {
                binding.tvEmpty.visibility = View.GONE
            }
        }

        // ── Player state → mini-player ────────────────────────────────────
        viewModel.playerState.observe(this) { state ->
            updateMiniPlayer(state)

            // Highlight the playing row in the list
            val songIndex = viewModel.songs.value?.indexOfFirst {
                it.id == state.currentSong?.id
            } ?: -1
            adapter.playingIndex = songIndex
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Mini-player
    // ─────────────────────────────────────────────────────────────────────

    private fun updateMiniPlayer(state: PlayerState) {
        val mini = binding.miniPlayer

        if (!state.hasSong) {
            mini.root.visibility = View.GONE
            return
        }

        if (mini.root.visibility != View.VISIBLE) {
            mini.root.visibility = View.VISIBLE
            mini.root.animateSlideUp()
        }

        val song = state.currentSong!!
        mini.tvMiniTitle.text  = song.title
        mini.tvMiniArtist.text = song.artist

        Glide.with(this)
            .load(song.albumArtUri)
            .placeholder(R.drawable.placeholder_album)
            .error(R.drawable.placeholder_album)
            .centerCrop()
            .into(mini.ivMiniArt)

        mini.btnMiniPlayPause.setImageResource(
            if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────────────

    private fun openNowPlaying() {
        startActivity(Intent(this, NowPlayingActivity::class.java))
        overridePendingTransition(R.anim.slide_up, R.anim.fade_out)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Permissions
    // ─────────────────────────────────────────────────────────────────────

    private fun checkAndRequestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        when {
            ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED -> viewModel.loadSongs()

            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(this,
                    "Storage permission is needed to find your music",
                    Toast.LENGTH_LONG).show()
                permissionLauncher.launch(permission)
            }

            else -> permissionLauncher.launch(permission)
        }
    }

    private fun showPermissionDenied() {
        binding.tvEmpty.visibility = View.VISIBLE
        binding.tvEmpty.text = "Permission denied.\nPlease grant storage access in Settings."
    }
}
