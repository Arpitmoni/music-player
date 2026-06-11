package com.musicplayer.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.musicplayer.R
import com.musicplayer.data.model.Song
import com.musicplayer.databinding.ItemSongBinding

/**
 * SongAdapter
 *
 * Uses [ListAdapter] (backed by [DiffUtil]) for efficient, animated updates.
 *
 * Each row shows:
 *  - Album art thumbnail (circular, with fade-in)
 *  - Song title + artist
 *  - Track duration
 *  - A playing indicator (animated equalizer) when this track is active
 */
class SongAdapter(
    private val onSongClick: (Song, Int) -> Unit
) : ListAdapter<Song, SongAdapter.SongViewHolder>(DIFF_CALLBACK) {

    /** Index of the currently playing song (-1 = none) */
    var playingIndex: Int = -1
        set(value) {
            val old = field
            field = value
            // Refresh only the two rows that changed (old + new playing)
            if (old >= 0) notifyItemChanged(old)
            if (value >= 0) notifyItemChanged(value)
        }

    // ─────────────────────────────────────────────────────────────────────
    // ViewHolder
    // ─────────────────────────────────────────────────────────────────────

    inner class SongViewHolder(val binding: ItemSongBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(song: Song, isPlaying: Boolean) = with(binding) {

            // ── Text ──────────────────────────────────────────────────────
            tvTitle.text    = song.title
            tvArtist.text   = song.artist
            tvDuration.text = song.formattedDuration()

            // ── Album art ─────────────────────────────────────────────────
            Glide.with(root.context)
                .load(song.albumArtUri)
                .placeholder(R.drawable.placeholder_album)
                .error(R.drawable.placeholder_album)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .into(ivAlbumArt)

            // ── Playing indicator ─────────────────────────────────────────
            if (isPlaying) {
                tvTitle.setTextColor(root.context.getColor(R.color.accent_purple))
                viewPlayingIndicator.visibility = android.view.View.VISIBLE
                root.setBackgroundResource(R.drawable.bg_song_item_active)
            } else {
                tvTitle.setTextColor(root.context.getColor(R.color.text_primary))
                viewPlayingIndicator.visibility = android.view.View.INVISIBLE
                root.setBackgroundResource(R.drawable.bg_song_item)
            }

            // ── Click ─────────────────────────────────────────────────────
            root.setOnClickListener {
                onSongClick(song, absoluteAdapterPosition)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // RecyclerView.Adapter overrides
    // ─────────────────────────────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(getItem(position), position == playingIndex)
    }

    // ─────────────────────────────────────────────────────────────────────
    // DiffUtil — only redraw rows that actually changed
    // ─────────────────────────────────────────────────────────────────────

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Song>() {
            override fun areItemsTheSame(a: Song, b: Song) = a.id == b.id
            override fun areContentsTheSame(a: Song, b: Song) = a == b
        }
    }
}
