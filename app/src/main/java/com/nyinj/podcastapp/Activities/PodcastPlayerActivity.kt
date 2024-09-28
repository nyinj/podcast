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
import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.nyinj.podcastapp.DataClass.Podcast
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
    private lateinit var titleTextView: TextView
    private lateinit var uploaderNameTextView: TextView
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MediaPlayerService.MediaPlayerBinder
            mediaPlayerService = binder.getService()
            serviceBound = true
            updateSeekBar()
            updatePlayPauseButton()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_podcast_player)

        // Retrieve extras from the Intent
        val audioUrl = intent.getStringExtra("AUDIO_URL")
        val podcastId = intent.getStringExtra("PODCAST_ID") // Add this to pass podcast ID

        // Ensure the audio URL is valid before starting the service
        if (audioUrl != null) {
            startMediaPlayerService(audioUrl)
        } else {
            Toast.makeText(this, "Audio URL is null", Toast.LENGTH_SHORT).show()
            finish() // Close activity if audio URL is null
        }
        Log.d("PodcastPlayerActivity", "Podcast ID: $podcastId")

        // Initialize UI elements
        playpauseButton = findViewById(R.id.playPauseButton)
        seekBar = findViewById(R.id.seek_bar)
        currentTime = findViewById(R.id.current_time)
        totalTime = findViewById(R.id.total_time)
        handler = Handler()
        backButton = findViewById(R.id.back_btn)
        titleTextView = findViewById(R.id.title_player)
        uploaderNameTextView = findViewById(R.id.uploader_player)

        // Fetch podcast details from Firebase
        fetchPodcastDetails(podcastId)

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

    private fun startMediaPlayerService(audioUrl: String) {
        val serviceIntent = Intent(this, MediaPlayerService::class.java).apply {
            putExtra("AUDIO_URL", audioUrl)
        }
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun fetchPodcastDetails(podcastId: String?) {
        if (podcastId != null) {
            val database = FirebaseDatabase.getInstance()
            val podcastRef: DatabaseReference = database.getReference("podcasts").child(podcastId)

            podcastRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Retrieve entire object as Podcast
                    val podcast = snapshot.getValue(Podcast::class.java)

                    // Log the podcast object to debug
                    Log.d("PodcastPlayerActivity", "Podcast: $podcast")

                    // Update UI
                    titleTextView.text = podcast?.title ?: "Unknown Title"
                    uploaderNameTextView.text = podcast?.uploaderName ?: "Unknown Uploader"
                } else {
                    Log.d("PodcastPlayerActivity", "Podcast not found in database")
                    Toast.makeText(this, "Podcast not found", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { exception ->
                Log.e("PodcastPlayerActivity", "Failed to retrieve podcast details", exception)
                Toast.makeText(this, "Failed to retrieve podcast details", Toast.LENGTH_SHORT)
                    .show()
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
            playpauseButton.setImageResource(if (mediaPlayerService.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
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
        super.onBackPressed() // Closes the activity
    }
}
