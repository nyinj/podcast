package com.nyinj.podcastapp.Services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.os.Build
import android.util.Log // Import logging
import androidx.core.app.NotificationCompat
import com.nyinj.podcastapp.R
import com.nyinj.podcastapp.Activities.MainActivity

class MediaPlayerService : Service() {

    private val binder = MediaPlayerBinder()
    private lateinit var mediaPlayer: MediaPlayer
    var isPlaying = false

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    inner class MediaPlayerBinder : Binder() {
        fun getService(): MediaPlayerService = this@MediaPlayerService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val audioUrl = intent?.getStringExtra("AUDIO_URL") ?: return START_NOT_STICKY
        Log.d("MediaPlayerService", "Received audio URL: $audioUrl") // Log audio URL

        // Release any existing MediaPlayer instance to avoid issues
        if (this::mediaPlayer.isInitialized) {
            Log.d("MediaPlayerService", "Releasing existing MediaPlayer")
            mediaPlayer.release()
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioUrl)
                Log.d("MediaPlayerService", "Setting data source to MediaPlayer")
                prepareAsync()
                Log.d("MediaPlayerService", "Preparing MediaPlayer asynchronously")

                setOnPreparedListener {
                    Log.d("MediaPlayerService", "MediaPlayer is prepared, starting playback")
                    start()
                    this@MediaPlayerService.isPlaying = true // Corrected the reference to isPlaying
                    startForegroundService()
                }

                setOnCompletionListener {
                    Log.d("MediaPlayerService", "Playback completed")
                    stopForeground(true)
                    stopSelf()
                    this@MediaPlayerService.isPlaying = false // Ensure isPlaying is false when finished
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("MediaPlayerService", "Error occurred: what=$what, extra=$extra")
                    stopForeground(true)
                    stopSelf()
                    this@MediaPlayerService.isPlaying = false // Ensure isPlaying is false on error
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("MediaPlayerService", "Error initializing MediaPlayer: ${e.message}")
            stopSelf()
        }

        return START_NOT_STICKY
    }

    fun skipForward() {
        if (this::mediaPlayer.isInitialized) {
            mediaPlayer.let {
                val currentPosition = it.currentPosition
                val newPosition = currentPosition + 10000 // Skip forward by 10 seconds (10,000 ms)
                Log.d("MediaPlayerService", "Skipping forward 10 seconds: $newPosition")
                if (newPosition < it.duration) {
                    it.seekTo(newPosition)
                }
            }
        }
    }

    fun skipBackward() {
        if (this::mediaPlayer.isInitialized) {
            mediaPlayer.let {
                val currentPosition = it.currentPosition
                val newPosition = currentPosition - 10000 // Skip backward by 10 seconds
                Log.d("MediaPlayerService", "Skipping backward 10 seconds: $newPosition")
                if (newPosition > 0) {
                    it.seekTo(newPosition)
                } else {
                    it.seekTo(0)
                }
            }
        }
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "media_playback", "Media Playback", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE)

        val notification: Notification = NotificationCompat.Builder(this, "media_playback")
            .setContentTitle("Podcast is playing")
            .setContentText("Your podcast is playing in the background")
            .setSmallIcon(R.drawable.ic_play)
            .setContentIntent(pendingIntent)
            .build()

        Log.d("MediaPlayerService", "Starting foreground service with notification")
        startForeground(1, notification)
    }

    fun playPause() {
        if (this::mediaPlayer.isInitialized) {
            if (isPlaying) {
                Log.d("MediaPlayerService", "Pausing playback")
                mediaPlayer.pause()
            } else {
                Log.d("MediaPlayerService", "Resuming playback")
                mediaPlayer.start()
            }
            isPlaying = !isPlaying
        }
    }

    fun seekTo(position: Int) {
        if (this::mediaPlayer.isInitialized && position >= 0 && position <= getDuration()) {
            Log.d("MediaPlayerService", "Seeking to position: $position")
            mediaPlayer.seekTo(position)
        }
    }

    fun getCurrentPosition(): Int {
        return if (this::mediaPlayer.isInitialized) {
            mediaPlayer.currentPosition
        } else {
            0
        }
    }

    fun getDuration(): Int {
        return if (this::mediaPlayer.isInitialized) {
            mediaPlayer.duration
        } else {
            0
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::mediaPlayer.isInitialized) {
            Log.d("MediaPlayerService", "Releasing MediaPlayer on service destroy")
            mediaPlayer.release()
        }
    }
}
