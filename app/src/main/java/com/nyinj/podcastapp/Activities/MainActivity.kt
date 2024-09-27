package com.nyinj.podcastapp.Activities

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.nyinj.podcastapp.Adapters.FragmentPageAdapter
import com.nyinj.podcastapp.R
import com.nyinj.podcastapp.Services.MediaPlayerService

class MainActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2
    private lateinit var adapter: FragmentPageAdapter
    private val tabTitles = arrayOf("Home", "Explore", "Browse", "You")
    private lateinit var auth: FirebaseAuth
    private lateinit var miniPlayer: RelativeLayout
    private lateinit var miniPlayerTitle: TextView
    private lateinit var miniPlayerPlayPauseButton: ImageButton
    private var mediaPlayerService: MediaPlayerService? = null
    private var isBound = false

    // Service connection to handle binding to MediaPlayerService
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MediaPlayerService.MediaPlayerBinder
            mediaPlayerService = binder.getService()
            isBound = true
            Log.d("MainActivity", "Service connected")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mediaPlayerService = null
            isBound = false
            Log.d("MainActivity", "Service disconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called")
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
        findViewById<TextView>(R.id.title_bar).text = tabTitles[0]

        miniPlayer = findViewById(R.id.mini_player)
        miniPlayerTitle = findViewById(R.id.mini_player_title)
        miniPlayerPlayPauseButton = findViewById(R.id.mini_player_play_pause)

        val podcastUrl = intent.getStringExtra("AUDIO_URL")
        Log.d("MainActivity", "Podcast URL: $podcastUrl")

        // Start MediaPlayerService if there's a podcast URL
        if (!podcastUrl.isNullOrEmpty()) {
            val serviceIntent = Intent(this, MediaPlayerService::class.java)
            serviceIntent.putExtra("AUDIO_URL", podcastUrl)
            startService(serviceIntent)
            bindService(serviceIntent, connection, BIND_AUTO_CREATE)

            // Display mini player
            miniPlayer.visibility = View.VISIBLE
            miniPlayerTitle.text = "Playing Podcast Title"
        }

        miniPlayerPlayPauseButton.setOnClickListener {
            if (isBound && mediaPlayerService != null) {
                mediaPlayerService?.playPause()
                updatePlayPauseIcon()
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
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

        // Authentication check
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }

    // Update the play/pause button in the mini player
    private fun updatePlayPauseIcon() {
        if (mediaPlayerService?.isPlaying == true) {
            miniPlayerPlayPauseButton.setImageResource(R.drawable.ic_pause)
        } else {
            miniPlayerPlayPauseButton.setImageResource(R.drawable.ic_play)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }


    fun setCurrentTab(index: Int) {
        viewPager2.currentItem = index
    }



}
