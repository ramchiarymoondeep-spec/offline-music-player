package com.example.musicplayer.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.musicplayer.R
import com.example.musicplayer.models.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicService : Service() {
    private val binder = LocalBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var currentSong: Song? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var onStateChangeListener: ((Boolean) -> Unit)? = null
    private var onPositionChangeListener: ((Int) -> Unit)? = null
    private var onSongChangeListener: ((Song?) -> Unit)? = null
    private var onPlaylistEndListener: (() -> Unit)? = null
    
    // For auto-play functionality
    private var playlist: MutableList<Song> = mutableListOf()
    private var currentIndex = 0
    private var isAutoPlayEnabled = true

    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    // Set the playlist for auto-play
    fun setPlaylist(songs: List<Song>) {
        playlist = songs.toMutableList()
    }

    // Enable/Disable auto-play
    fun setAutoPlayEnabled(enabled: Boolean) {
        isAutoPlayEnabled = enabled
    }

    fun playSong(song: Song) {
        currentSong = song
        // Find the song index in the playlist for auto-play
        currentIndex = playlist.indexOfFirst { it.path == song.path }
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(song.path)
                prepare()
                start()
                // Set listener for when song finishes
                setOnCompletionListener {
                    if (isAutoPlayEnabled) {
                        playNext()
                    } else {
                        onStateChangeListener?.invoke(false)
                    }
                }
            }
            onStateChangeListener?.invoke(true)
            onSongChangeListener?.invoke(song)
            startForegroundNotification()
            updateProgress()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Play next song in playlist
    fun playNext() {
        if (playlist.isEmpty()) return
        
        currentIndex = (currentIndex + 1) % playlist.size
        
        // If we've looped back to the start, notify
        if (currentIndex == 0) {
            onPlaylistEndListener?.invoke()
        }
        
        playSong(playlist[currentIndex])
    }

    // Play previous song in playlist
    fun playPrevious() {
        if (playlist.isEmpty()) return
        
        currentIndex = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
        playSong(playlist[currentIndex])
    }

    fun pausePlayback() {
        mediaPlayer?.apply {
            if (isPlaying) {
                pause()
                onStateChangeListener?.invoke(false)
            }
        }
    }

    fun resumePlayback() {
        mediaPlayer?.apply {
            if (!isPlaying) {
                start()
                onStateChangeListener?.invoke(true)
            }
        }
    }

    fun togglePlayback() {
        mediaPlayer?.apply {
            if (isPlaying) {
                pausePlayback()
            } else {
                resumePlayback()
            }
        }
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    fun getDuration(): Int = mediaPlayer?.duration ?: 0
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false
    fun getCurrentSong(): Song? = currentSong
    fun getCurrentIndex(): Int = currentIndex

    fun setOnStateChangeListener(listener: (Boolean) -> Unit) {
        onStateChangeListener = listener
    }

    fun setOnPositionChangeListener(listener: (Int) -> Unit) {
        onPositionChangeListener = listener
    }

    fun setOnSongChangeListener(listener: (Song?) -> Unit) {
        onSongChangeListener = listener
    }

    // Set listener for when playlist ends
    fun setOnPlaylistEndListener(listener: () -> Unit) {
        onPlaylistEndListener = listener
    }

    private fun updateProgress() {
        serviceScope.launch {
            while (mediaPlayer?.isPlaying == true) {
                onPositionChangeListener?.invoke(getCurrentPosition())
                delay(100)
            }
        }
    }

    private fun startForegroundNotification() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "music_channel",
                "Music Playback",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(android.app.NotificationManager::class.java)?.createNotificationChannel(channel)
        }

                val isPlaying = mediaPlayer?.isPlaying == true
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseAction = if (isPlaying) "PAUSE" else "PLAY"

        val notification = NotificationCompat.Builder(this, "music_channel")
            .setContentTitle(currentSong?.title ?: "Playing")
            .setContentText(currentSong?.artist ?: "Unknown Artist")
            .setSmallIcon(R.drawable.ic_music_code) 
            .addAction(android.R.drawable.ic_media_previous, "Previous", getPendingIntent("PREVIOUS"))
            .addAction(playPauseIcon, "Play/Pause", getPendingIntent(playPauseAction))
            .addAction(android.R.drawable.ic_media_next, "Next", getPendingIntent("NEXT"))
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2) 
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)

    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }     
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "PLAY" -> resumePlayback()
            "PAUSE" -> pausePlayback()
            "NEXT" -> playNext()
            "PREVIOUS" -> playPrevious()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

}

    
