package com.nyinj.podcastapp.Activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.nyinj.podcastapp.Adapters.FragmentPageAdapter
import com.nyinj.podcastapp.R

class MainActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2
    private lateinit var adapter: FragmentPageAdapter
    private val tabTitles = arrayOf("Home", "Explore", "Browse", "Library", "You")
    private lateinit var auth: FirebaseAuth

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
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_library))
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_you))

        viewPager2.adapter=adapter

        findViewById<TextView>(R.id.title_bar).text = tabTitles[0]

        tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    viewPager2.currentItem=tab.position

                    findViewById<TextView>(R.id.title_bar).text = tabTitles[tab.position]
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

        })

        viewPager2.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tabLayout.selectTab(tabLayout.getTabAt(position))
            }

        })


        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null){
            val intent = Intent (this, Login::class.java )
            startActivity(intent)
        }

    }
}