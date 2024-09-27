package com.nyinj.podcastapp.Activities

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.nyinj.podcastapp.Adapters.FragmentPageAdapter
import com.nyinj.podcastapp.R
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2
    private lateinit var adapter: FragmentPageAdapter
    private val tabTitles = arrayOf("Home", "Explore", "Browse", "You")
    private lateinit var auth: FirebaseAuth
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var miniPlayer: RelativeLayout
    private lateinit var miniPlayerTitle: TextView
    private lateinit var miniPlayerPlayPauseButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called")
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        tabLayout = findViewById(R.id.tab_layout)
        viewPager2 = findViewById(R.id.viewpager2)

        adapter = FragmentPageAdapter(supportFragmentManager, lifecycle)

        // Set icons for tabs instead of text
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_home))
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_explore))
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_browse))
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_you))

        viewPager2.adapter = adapter

        // Display the title of the selected tab
        findViewById<TextView>(R.id.title_bar).text = tabTitles[0]

        // Initialize mini player views
        miniPlayer = findViewById(R.id.mini_player)
        miniPlayerTitle = findViewById(R.id.mini_player_title)
        miniPlayerPlayPauseButton = findViewById(R.id.mini_player_play_pause)

        // Handle incoming podcast URL from intent
        val podcastUrl = intent.getStringExtra("AUDIO_URL")
        Log.d("MainActivity", "Podcast URL: $podcastUrl")

        if (!podcastUrl.isNullOrEmpty()) {
            initializeMediaPlayer(podcastUrl)
        } else {
            Log.w("MainActivity", "Podcast URL is null or empty")
        }

        // Play/Pause button logic for the mini player
        miniPlayerPlayPauseButton.setOnClickListener {
            if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                Log.d("MainActivity", "Pausing media player")
                mediaPlayer.pause()
                miniPlayerPlayPauseButton.setImageResource(R.drawable.ic_play)
            } else if (::mediaPlayer.isInitialized) {
                Log.d("MainActivity", "Starting media player")
                mediaPlayer.start()
                miniPlayerPlayPauseButton.setImageResource(R.drawable.ic_pause)
            } else {
                Log.e("MainActivity", "MediaPlayer is not initialized")
            }
        }

        // Handle tab changes
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    Log.d("MainActivity", "Tab selected: ${tabTitles[tab.position]}")
                    viewPager2.currentItem = tab.position
                    findViewById<TextView>(R.id.title_bar).text = tabTitles[tab.position]
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                Log.d("MainActivity", "Page selected: ${tabTitles[position]}")
                tabLayout.selectTab(tabLayout.getTabAt(position))
            }
        })

        // Authentication check for user login status
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.w("MainActivity", "User not logged in, redirecting to login")
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }

    // Initialize the media player with the podcast URL
    private fun initializeMediaPlayer(url: String) {
        Log.d("MainActivity", "Initializing media player with URL: $url")
        mediaPlayer = MediaPlayer()

        try {
            mediaPlayer.setDataSource(url)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener {
                Log.d("MainActivity", "Media player prepared, starting playback")
                mediaPlayer.start()

                // Show mini player when the podcast starts playing
                miniPlayer.visibility = View.VISIBLE
                miniPlayerTitle.text = "Playing Podcast Title" // Replace with the actual title from metadata

                // Set play button to "Pause" as media starts playing
                miniPlayerPlayPauseButton.setImageResource(R.drawable.ic_pause)
            }

            mediaPlayer.setOnCompletionListener {
                Log.d("MainActivity", "Media player completed, hiding mini player")
                // Hide mini player when the podcast finishes
                miniPlayer.visibility = View.GONE
            }
        } catch (e: IOException) {
            Log.e("MainActivity", "Error initializing media player", e)
        }
    }

    // Set the current tab programmatically
    fun setCurrentTab(index: Int) {
        Log.d("MainActivity", "Setting current tab to index: $index")
        viewPager2.currentItem = index
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy called, releasing media player")
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }
}
