package com.nyinj.podcastapp.Activities

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import android.os.Handler
import com.nyinj.podcastapp.R

class PodcastPlayerActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var seekBar: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView
    private lateinit var handler: Handler
    private lateinit var playpauseButton : ImageButton
    private lateinit var backButton: ImageButton
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_podcast_player)

        // Get the podcast URL passed from the BrowseFragment
        val audioUrl = intent.getStringExtra("AUDIO_URL")

        // Initialize UI elements
        playpauseButton = findViewById(R.id.playPauseButton)
        seekBar = findViewById(R.id.seek_bar)
        currentTime = findViewById(R.id.current_time)
        totalTime = findViewById(R.id.total_time)
        handler = Handler()
        backButton=findViewById(R.id.back_btn)

        // Initialize MediaPlayer
        mediaPlayer = MediaPlayer()

        backButton.setOnClickListener{
            finish() //to close the activity
        }

        try {
            mediaPlayer.setDataSource(audioUrl) // Load the audio file from URL
            mediaPlayer.prepareAsync() // Prepare asynchronously
            mediaPlayer.setOnPreparedListener {
                seekBar.max = mediaPlayer.duration
                totalTime.text = formatTime(mediaPlayer.duration)
            }

            mediaPlayer.setOnCompletionListener {
                playpauseButton.setImageResource(R.drawable.ic_play) // Reset to play icon
                isPlaying = false
            }

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading podcast", Toast.LENGTH_SHORT).show()
        }

        // Play/Pause button action
        playpauseButton.setOnClickListener {
            if (isPlaying) {
                mediaPlayer.pause()
                playpauseButton.setImageResource(R.drawable.ic_play)
            } else {
                mediaPlayer.start()
                updateTime()
                playpauseButton.setImageResource(R.drawable.ic_pause)
            }
            isPlaying = !isPlaying
        }

        // SeekBar change listener
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                    currentTime.text = formatTime(mediaPlayer.currentPosition)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateTime() {
        handler.postDelayed({
            if (mediaPlayer.isPlaying) {
                seekBar.progress = mediaPlayer.currentPosition
                currentTime.text = formatTime(mediaPlayer.currentPosition)
                updateTime()
            }
        }, 1000)
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release() // Release resources
        }
    }
}