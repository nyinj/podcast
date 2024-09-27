package com.nyinj.podcastapp.Activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.os.Handler
import android.os.IBinder
import com.nyinj.podcastapp.R
import com.nyinj.podcastapp.Services.MediaPlayerService

class PodcastPlayerActivity : AppCompatActivity() {

    private lateinit var seekBar: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView
    private lateinit var handler: Handler
    private lateinit var playpauseButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var forwardButton: ImageButton
    private lateinit var backButton10s: ImageButton
    private lateinit var mediaPlayerService: MediaPlayerService
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MediaPlayerService.MediaPlayerBinder
            mediaPlayerService = binder.getService()
            serviceBound = true
            updateSeekBar()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_podcast_player)

        val audioUrl = intent.getStringExtra("AUDIO_URL")
        val serviceIntent = Intent(this, MediaPlayerService::class.java).apply {
            putExtra("AUDIO_URL", audioUrl)
        }
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Initialize UI elements
        playpauseButton = findViewById(R.id.playPauseButton)
        seekBar = findViewById(R.id.seek_bar)
        currentTime = findViewById(R.id.current_time)
        totalTime = findViewById(R.id.total_time)
        handler = Handler()
        backButton = findViewById(R.id.back_btn)

        backButton.setOnClickListener { finish() }

        playpauseButton.setOnClickListener {
            if (serviceBound) {
                mediaPlayerService.playPause()
                updatePlayPauseButton()
            }
        }

        // SeekBar change listener
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && serviceBound) {
                    mediaPlayerService.seekTo(progress)
                    currentTime.text = formatTime(mediaPlayerService.getCurrentPosition())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Initialize new buttons
        forwardButton = findViewById(R.id.forward10s)
        backButton10s = findViewById(R.id.back10s)

        forwardButton.setOnClickListener {
            if (serviceBound) {
                mediaPlayerService.skipForward() // Skip 10 seconds
            }
        }

        backButton10s.setOnClickListener {
            if (serviceBound) {
                mediaPlayerService.skipBackward() // Go back 10 seconds
            }
        }
    }

    private fun updateSeekBar() {
        if (serviceBound) {
            seekBar.max = mediaPlayerService.getDuration()
            seekBar.progress = mediaPlayerService.getCurrentPosition()
            totalTime.text = formatTime(mediaPlayerService.getDuration())
            currentTime.text = formatTime(mediaPlayerService.getCurrentPosition())

            handler.postDelayed({ updateSeekBar() }, 1000)
        }
    }

    private fun updatePlayPauseButton() {
        if (serviceBound) {
            if (mediaPlayerService.isPlaying) {
                playpauseButton.setImageResource(R.drawable.ic_pause)
            } else {
                playpauseButton.setImageResource(R.drawable.ic_play)
            }
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    override fun onBackPressed() {
        if (serviceBound && mediaPlayerService.isPlaying) {
            mediaPlayerService.playPause()
        }
        super.onBackPressed() // Closes the activity
    }
}
