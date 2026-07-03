package com.example.musicplayer

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.adapter.SongAdapter
import com.example.musicplayer.databinding.ActivityMainBinding
import com.example.musicplayer.models.Song
import com.example.musicplayer.service.MusicService
import com.example.musicplayer.util.MusicManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var musicService: MusicService? = null
    private var isBound = false
    private var songs = mutableListOf<Song>()
    private var currentSongIndex = 0
    private lateinit var adapter: SongAdapter

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.LocalBinder
            musicService = binder.getService()
            isBound = true
            setupMusicServiceListeners()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissions()
        setupUI()
        bindMusicService()
        loadSongs()
    }

    private fun setupUI() {
        adapter = SongAdapter(songs) { song, index ->
            currentSongIndex = index
            musicService?.setPlaylist(songs)
            musicService?.playSong(song)
        }

        binding.songListRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        setupPlayerControls()
    }

    private fun setupPlayerControls() {
        binding.playPauseButton.setOnClickListener {
            musicService?.togglePlayback()
        }

        binding.nextButton.setOnClickListener {
            musicService?.playNext()
        }

        binding.previousButton.setOnClickListener {
            musicService?.playPrevious()
        }

        binding.progressSeekBar.setOnSeekBarChangeListener(
            object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        musicService?.seekTo(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            }
        )
    }

    private fun setupMusicServiceListeners() {
        musicService?.setOnStateChangeListener { isPlaying ->
            updatePlayPauseButton(isPlaying)
        }

        musicService?.setOnPositionChangeListener { position ->
            binding.progressSeekBar.progress = position
            binding.currentTimeTextView.text = MusicManager.formatDuration(position)
        }

        musicService?.setOnSongChangeListener { song ->
            song?.let {
                binding.songTitleTextView.text = it.title
                binding.artistNameTextView.text = it.artist
                binding.totalDurationTextView.text = MusicManager.formatDuration(it.duration.toInt())
                binding.progressSeekBar.max = it.duration.toInt()
            }
        }
    }

    private fun loadSongs() {
        lifecycleScope.launch {
            songs = MusicManager.getAllSongs(this@MainActivity).toMutableList()
            adapter.updateSongs(songs)
        }
    }

    private fun playNext() {
        if (songs.isNotEmpty()) {
            currentSongIndex = (currentSongIndex + 1) % songs.size
            musicService?.playSong(songs[currentSongIndex])
            adapter.setCurrentSong(currentSongIndex)
        }
    }

    private fun playPrevious() {
        if (songs.isNotEmpty()) {
            currentSongIndex = if (currentSongIndex == 0) songs.size - 1 else currentSongIndex - 1
            musicService?.playSong(songs[currentSongIndex])
            adapter.setCurrentSong(currentSongIndex)
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        binding.playPauseButton.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
    }

    private fun bindMusicService() {
        val intent = Intent(this, MusicService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                101
            )
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                101
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}
