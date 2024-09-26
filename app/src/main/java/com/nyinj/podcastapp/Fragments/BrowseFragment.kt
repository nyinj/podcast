package com.nyinj.podcastapp.Fragments

import com.nyinj.podcastapp.DataClass.Podcast
import com.nyinj.podcastapp.Adapters.PodcastAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nyinj.podcastapp.Activities.PodcastPlayerActivity
import com.nyinj.podcastapp.R

class BrowseFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var podcastAdapter: PodcastAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var podcastList: MutableList<Podcast>
    private lateinit var progressBar: ProgressBar  // ProgressBar reference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_browse, container, false)

        database = FirebaseDatabase.getInstance()

        // Initialize views
        recyclerView = view.findViewById(R.id.exploreRecyclerView)
        progressBar = view.findViewById(R.id.explore_progress_bar)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        podcastList = mutableListOf()

        // Initialize adapter and handle play and favorite button clicks
        podcastAdapter = PodcastAdapter(
            podcastList,
            onPlayButtonClick = { podcast ->
                // Handle play button click
                val intent = Intent(requireContext(), PodcastPlayerActivity::class.java).apply {
                    putExtra("AUDIO_URL", podcast.audioUrl)
                    putExtra("PODCAST_TITLE", podcast.title)
                }
                startActivity(intent)
            }
        )

        recyclerView.adapter = podcastAdapter

        // Fetch podcasts and show progress bar while loading
        fetchPodcasts()

        return view
    }

    private var lastFetchedKey: String? = null // Track the last fetched key

    private fun fetchPodcasts(limit: Int = 20) {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val podcastRef = database.getReference("podcasts").limitToFirst(limit)

        if (lastFetchedKey != null) {
            podcastRef.startAt(lastFetchedKey)
        }

        podcastRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                podcastList.clear()

                for (data in snapshot.children) {
                    val podcast = data.getValue(Podcast::class.java)
                    if (podcast != null && podcast.uploaderId != currentUserId) {
                        podcastList.add(podcast)
                        lastFetchedKey = data.key // Update the last fetched key
                    }
                }
                podcastAdapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Failed to load podcasts", Toast.LENGTH_SHORT).show()
            }
        })
    }

}