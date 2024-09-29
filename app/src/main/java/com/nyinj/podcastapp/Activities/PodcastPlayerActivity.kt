package com.nyinj.podcastapp.Activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
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
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
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
    private lateinit var audioManager: AudioManager
    private lateinit var mediaPlayerService: MediaPlayerService
    private lateinit var titleTextView: TextView
    private lateinit var uploaderNameTextView: TextView
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var podcastCoverImage: ImageView


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

        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION  or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        // Retrieve extras from the Intent
        val audioUrl = intent.getStringExtra("AUDIO_URL")
        val podcastId = intent.getStringExtra("PODCAST_ID") // Add this to pass podcast ID
        podcastCoverImage = findViewById(R.id.player_pic) // Add this


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

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Optional: Stop updating the SeekBar while the user is dragging it
                handler.removeCallbacksAndMessages(null)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Restart the update when the user stops dragging the SeekBar
                updateSeekBar()
            }
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

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Find the SeekBar for volume control
        volumeSeekBar = findViewById(R.id.volumeSeekBar)

        // Set the max value of the SeekBar to the device's max volume for STREAM_MUSIC
        volumeSeekBar.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        // Set the current progress to the current volume level
        volumeSeekBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        // Set up a listener for when the user changes the SeekBar progress
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Change the device's media volume according to the SeekBar progress
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Optionally, you can handle any events when user starts adjusting the SeekBar
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Optionally, you can handle any events when user stops adjusting the SeekBar
            }
        })
    }


    // Optional: Override hardware volume button behavior
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                // Raise the volume and adjust the SeekBar accordingly
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
                updateVolumeSeekBar()
                true
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                // Lower the volume and adjust the SeekBar accordingly
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
                updateVolumeSeekBar()
                true
            }

            else -> super.onKeyDown(keyCode, event)

        }
    }

    // Function to update the SeekBar when volume buttons are pressed
    private fun updateVolumeSeekBar() {
        volumeSeekBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
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
                    val podcast = snapshot.getValue(Podcast::class.java)

                    Log.d("PodcastPlayerActivity", "Podcast: $podcast")
                    Log.d("PodcastPlayerActivity", "Cover URL: ${podcast?.coverUrl}")

                    titleTextView.text = podcast?.title ?: "Unknown Title"
                    uploaderNameTextView.text = podcast?.uploaderName ?: "Unknown Uploader"

                    if (!podcast?.coverUrl.isNullOrEmpty()) {
                        if (podcast != null) {
                            Glide.with(this)
                                .load(podcast.coverUrl)
                                .placeholder(R.drawable.unknownpodcast)
                                .error(R.drawable.unknownpodcast)
                                .into(podcastCoverImage)
                        } // Use the initialized ImageView
                    } else {
                        podcastCoverImage.setImageResource(R.drawable.unknownpodcast)
                    }
                } else {
                    Log.d("PodcastPlayerActivity", "Podcast not found in database")
                    Toast.makeText(this, "Podcast not found", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { exception ->
                Log.e("PodcastPlayerActivity", "Failed to retrieve podcast details", exception)
                Toast.makeText(this, "Failed to retrieve podcast details", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun updateSeekBar() {
        if (serviceBound) {
            seekBar.max = mediaPlayerService.getDuration()
            seekBar.progress = mediaPlayerService.getCurrentPosition()
            totalTime.text = formatTime(mediaPlayerService.getDuration())
            currentTime.text = formatTime(mediaPlayerService.getCurrentPosition())

            handler.postDelayed({ updateSeekBar() }, 1000) // Update every second
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
