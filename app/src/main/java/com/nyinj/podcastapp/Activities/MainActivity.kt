package com.nyinj.podcastapp.Activities

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
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
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        tabLayout = findViewById(R.id.tab_layout)
        viewPager2 = findViewById(R.id.viewpager2)

        adapter = FragmentPageAdapter(supportFragmentManager, lifecycle)

        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_home))
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_explore))
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_browse))
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_you))

        viewPager2.adapter = adapter

        findViewById<TextView>(R.id.title_bar).text = tabTitles[0]

        // Initialize mini player views
        miniPlayer = findViewById(R.id.mini_player)
        miniPlayerTitle = findViewById(R.id.mini_player_title)
        miniPlayerPlayPauseButton = findViewById(R.id.mini_player_play_pause)

        // Start playing a podcast (assume the podcast URL is passed through an Intent)
        val podcastUrl = intent.getStringExtra("AUDIO_URL")

        if (!podcastUrl.isNullOrEmpty()) {
            initializeMediaPlayer(podcastUrl)
        }

        miniPlayerPlayPauseButton.setOnClickListener {
            if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                miniPlayerPlayPauseButton.setImageResource(R.drawable.ic_play)
            } else if (::mediaPlayer.isInitialized) {
                mediaPlayer.start()
                miniPlayerPlayPauseButton.setImageResource(R.drawable.ic_pause)
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
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
                tabLayout.selectTab(tabLayout.getTabAt(position))
            }
        })

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }

    // Initialize MediaPlayer with podcast URL
    private fun initializeMediaPlayer(url: String) {
        mediaPlayer = MediaPlayer()

        try {
            mediaPlayer.setDataSource(url)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener {
                mediaPlayer.start()
                miniPlayer.visibility = View.VISIBLE // Show mini player when ready
                miniPlayerTitle.text = "Playing Podcast Title" // Replace with actual title
            }
            mediaPlayer.setOnCompletionListener {
                miniPlayer.visibility = View.GONE // Hide mini player when podcast is done
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun setCurrentTab(index: Int) {
        viewPager2.currentItem = index
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }
}
