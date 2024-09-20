package com.nyinj.podcastapp

import Podcast
import PodcastAdapter
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

class ExploreFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var podcastAdapter: PodcastAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var podcastList: MutableList<Podcast>
    private lateinit var progressBar: ProgressBar  // ProgressBar reference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_explore, container, false)

        database = FirebaseDatabase.getInstance()  // Add this line to initialize Firebase Database

        // Initialize views
        recyclerView = view.findViewById(R.id.exploreRecyclerView)
        progressBar = view.findViewById(R.id.explore_progress_bar)  // Find the ProgressBar

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        podcastList = mutableListOf()
        podcastAdapter = PodcastAdapter(podcastList)
        recyclerView.adapter = podcastAdapter

        // Fetch podcasts and show progress bar while loading
        fetchPodcasts()

        return view
    }

    private fun fetchPodcasts() {
        // Show the progress bar before loading
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val podcastRef = database.getReference("podcasts")

        podcastRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                podcastList.clear()  // Clear the list before adding new data

                for (data in snapshot.children) {
                    val podcast = data.getValue(Podcast::class.java)
                    if (podcast != null && podcast.uploaderId != currentUserId) {
                        podcastList.add(podcast)
                    }
                }
                podcastAdapter.notifyDataSetChanged()

                // Hide the progress bar and show the RecyclerView when data is loaded
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }

            override fun onCancelled(error: DatabaseError) {
                // Hide the progress bar and show a failure message
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Failed to load podcasts", Toast.LENGTH_SHORT).show()
            }
        })
    }
}