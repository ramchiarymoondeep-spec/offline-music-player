package com.example.musicplayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.databinding.SongItemBinding
import com.example.musicplayer.models.Song
import com.example.musicplayer.util.MusicManager

class SongAdapter(
    private val songs: MutableList<Song>,
    private val onSongClick: (Song, Int) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    private var currentSongIndex = -1

    inner class SongViewHolder(private val binding: SongItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song, index: Int) {
            binding.apply {
                songTitleTextView.text = song.title
                artistNameTextView.text = song.artist
                durationTextView.text = MusicManager.formatDuration(song.duration.toInt())

                if (index == currentSongIndex) {
                    itemView.setBackgroundColor(itemView.context.getColor(R.color.primary_color))
                } else {
                    itemView.setBackgroundColor(itemView.context.getColor(android.R.color.background_light))
                }

                root.setOnClickListener {
                    onSongClick(song, index)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = SongItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(songs[position], position)
    }

    override fun getItemCount(): Int = songs.size

    fun updateSongs(newSongs: List<Song>) {
        songs.clear()
        songs.addAll(newSongs)
        notifyDataSetChanged()
    }

    fun setCurrentSong(index: Int) {
        val previousIndex = currentSongIndex
        currentSongIndex = index
        if (previousIndex != -1) notifyItemChanged(previousIndex)
        notifyItemChanged(index)
    }
}
