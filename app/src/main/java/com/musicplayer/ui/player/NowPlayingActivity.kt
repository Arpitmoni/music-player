package com.musicplayer.ui.player

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.musicplayer.R
import com.musicplayer.data.model.PlayerState
import com.musicplayer.data.model.currentSong
import com.musicplayer.data.model.isPlaying
import com.musicplayer.databinding.ActivityNowPlayingBinding
import com.musicplayer.service.PlayerController
import com.musicplayer.ui.viewmodel.NowPlayingViewModel
import com.musicplayer.utils.formatMillis

/**
 * NowPlayingActivity
 *
 * Full-screen player featuring:
 *  - Large animated album art (pulse on play)
 *  - Song title + artist
 *  - Seekbar with current / total time labels
 *  - Play/Pause, Previous, Next controls
 *  - Back button → dismisses with slide-down transition
 */
class NowPlayingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNowPlayingBinding
    private val viewModel: NowPlayingViewModel by viewModels()

    /** True while the user is dragging the seek bar (block position updates) */
    private var isUserSeeking = false

    // ─────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNowPlayingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupControls()
        setupSeekBar()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        PlayerController.bind(this)
    }

    override fun onStop() {
        super.onStop()
        PlayerController.unbind(this)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.slide_down)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────

    private fun setupControls() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnPlayPause.setOnClickListener {
            viewModel.togglePlayPause()
        }
        binding.btnNext.setOnClickListener {
            viewModel.skipToNext()
        }
        binding.btnPrevious.setOnClickListener {
            viewModel.skipToPrevious()
        }
    }

    private fun setupSeekBar() {
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isUserSeeking = true
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Show position label while dragging
                    binding.tvCurrentTime.text = formatMillis(progress.toLong())
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                viewModel.seekTo(seekBar.progress.toLong())
                isUserSeeking = false
            }
        })
    }

    // ─────────────────────────────────────────────────────────────────────
    // Observers
    // ─────────────────────────────────────────────────────────────────────

    private fun observeViewModel() {

        viewModel.playerState.observe(this) { state ->
            val song = state.currentSong ?: return@observe

            // ── Text ──────────────────────────────────────────────────────
            binding.tvSongTitle.text  = song.title
            binding.tvArtistName.text = song.artist
            binding.tvAlbumName.text  = song.album

            // ── Album art ─────────────────────────────────────────────────
            Glide.with(this)
                .load(song.albumArtUri)
                .placeholder(R.drawable.placeholder_album_large)
                .error(R.drawable.placeholder_album_large)
                .centerCrop()
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?,
                        target: Target<Drawable>?, isFirstResource: Boolean) = false
                    override fun onResourceReady(resource: Drawable?, model: Any?,
                        target: Target<Drawable>?, ds: DataSource?, isFirst: Boolean): Boolean {
                        return false
                    }
                })
                .into(binding.ivAlbumArt)

            // ── Play/Pause button icon ─────────────────────────────────────
            binding.btnPlayPause.setImageResource(
                if (state.isPlaying) R.drawable.ic_pause_circle
                else                 R.drawable.ic_play_circle
            )

            // ── Album art animation: scale up when playing ─────────────────
            val scale = if (state.isPlaying) 1f else 0.85f
            binding.ivAlbumArt.animate()
                .scaleX(scale).scaleY(scale)
                .setDuration(300)
                .start()

            // ── Seekbar total duration ────────────────────────────────────
            if (state is PlayerState.Playing || state is PlayerState.Paused) {
                val dur = when (state) {
                    is PlayerState.Playing -> state.duration
                    is PlayerState.Paused  -> state.duration
                    else -> 0L
                }
                binding.seekBar.max = dur.toInt()
                binding.tvTotalTime.text = formatMillis(dur)
            }
        }

        viewModel.progress.observe(this) { (position, duration) ->
            if (!isUserSeeking) {
                binding.seekBar.progress = position.toInt()
                binding.seekBar.max      = duration.toInt()
                binding.tvCurrentTime.text = formatMillis(position)
                binding.tvTotalTime.text   = formatMillis(duration)
            }
        }
    }
}
