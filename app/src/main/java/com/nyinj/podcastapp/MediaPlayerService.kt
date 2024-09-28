package com.nyinj.podcastapp.Services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nyinj.podcastapp.R
import com.nyinj.podcastapp.Activities.MainActivity

class MediaPlayerService : Service() {

    private val binder = MediaPlayerBinder()
    private lateinit var mediaPlayer: MediaPlayer
    private var podcastTitle: String? = null
    var isPlaying = true

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    inner class MediaPlayerBinder : Binder() {
        fun getService(): MediaPlayerService = this@MediaPlayerService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val audioUrl = intent?.getStringExtra("AUDIO_URL") ?: return START_NOT_STICKY
        podcastTitle = intent.getStringExtra("PODCAST_TITLE") ?: "Unknown Podcast"
        Log.d("MediaPlayerService", "Received audio URL: $audioUrl")

        if (this::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }

        initializeMediaPlayer(audioUrl)
        return START_NOT_STICKY
    }

    private fun initializeMediaPlayer(audioUrl: String) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(audioUrl)
                prepareAsync()
                setOnPreparedListener {
                    start()
                    this@MediaPlayerService.isPlaying = true
                    startForegroundService()
                }
                setOnCompletionListener {
                    stopForeground(true)
                    stopSelf()
                    this@MediaPlayerService.isPlaying = false
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("MediaPlayerService", "Error: what=$what, extra=$extra")
                    stopForeground(true)
                    stopSelf()
                    this@MediaPlayerService.isPlaying = false
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("MediaPlayerService", "Error initializing MediaPlayer: ${e.message}")
            stopSelf()
        }
    }

    fun playPause() {
        if (this::mediaPlayer.isInitialized) {
            if (isPlaying) {
                mediaPlayer.pause()
            } else {
                mediaPlayer.start()
            }
            isPlaying = !isPlaying
            updateNotification()
        }
    }

    // Method to get the current position
    fun getCurrentPosition(): Int {
        return if (this::mediaPlayer.isInitialized) mediaPlayer.currentPosition else 0
    }

    // Method to get the duration of the media
    fun getDuration(): Int {
        return if (this::mediaPlayer.isInitialized) mediaPlayer.duration else 0
    }

    // Method to seek to a specific position
    fun seekTo(position: Int) {
        if (this::mediaPlayer.isInitialized) mediaPlayer.seekTo(position)
    }

    // Method to skip forward 10 seconds
    fun skipForward() {
        val newPosition = mediaPlayer.currentPosition + 10000 // 10 seconds forward
        mediaPlayer.seekTo(newPosition.coerceAtMost(mediaPlayer.duration))
    }

    // Method to skip backward 10 seconds
    fun skipBackward() {
        val newPosition = mediaPlayer.currentPosition - 10000 // 10 seconds back
        mediaPlayer.seekTo(newPosition.coerceAtLeast(0))
    }

    private fun startForegroundService() {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, "media_playback")
            .setContentTitle("Playing: $podcastTitle")
            .setContentText("Your podcast is playing in the background")
            .setSmallIcon(R.drawable.ic_play)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_pause, "Pause", getPlayPauseAction())
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        val notification = NotificationCompat.Builder(this, "media_playback")
            .setContentTitle("Playing: $podcastTitle")
            .setContentText("Your podcast is playing in the background")
            .setSmallIcon(R.drawable.ic_play)
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pause" else "Play",
                getPlayPauseAction()
            )
            .setOngoing(isPlaying)
            .build()

        notificationManager.notify(1, notification)
    }

    private fun getPlayPauseAction(): PendingIntent {
        val intent = Intent(this, MediaPlayerService::class.java).apply {
            action = if (isPlaying) "ACTION_PAUSE" else "ACTION_PLAY"
        }
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "media_playback", "Media Playback", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }

    fun getCurrentPodcastTitle(): String? {
        return podcastTitle

    }
}
