package com.example.musicplayer.service

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

    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun playSong(song: Song) {
        currentSong = song
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(song.path)
                prepare()
                start()
            }
            onStateChangeListener?.invoke(true)
            onSongChangeListener?.invoke(song)
            startForegroundNotification()
            updateProgress()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    fun setOnStateChangeListener(listener: (Boolean) -> Unit) {
        onStateChangeListener = listener
    }

    fun setOnPositionChangeListener(listener: (Int) -> Unit) {
        onPositionChangeListener = listener
    }

    fun setOnSongChangeListener(listener: (Song?) -> Unit) {
        onSongChangeListener = listener
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
        val notification = NotificationCompat.Builder(this, "music_channel")
            .setContentTitle(currentSong?.title ?: "Playing")
            .setContentText(currentSong?.artist ?: "Unknown Artist")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}
