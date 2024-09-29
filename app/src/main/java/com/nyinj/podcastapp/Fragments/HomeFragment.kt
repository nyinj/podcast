package com.nyinj.podcastapp.Fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nyinj.podcastapp.Activities.MainActivity
import com.nyinj.podcastapp.Activities.PodcastPlayerActivity
import com.nyinj.podcastapp.Adapters.PodcastAdapter
import com.nyinj.podcastapp.DataClass.Podcast
import com.nyinj.podcastapp.R

class HomeFragment : Fragment() {

    private lateinit var searchInput: TextInputEditText
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var podcastAdapter: PodcastAdapter
    private var podcastsList = mutableListOf<Podcast>()
    private var filteredPodcastsList = mutableListOf<Podcast>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize views
        searchInput = view.findViewById(R.id.search_input)
        searchResultsRecyclerView = view.findViewById(R.id.search_results_recycler_view)

        // Initialize RecyclerView
        podcastAdapter = PodcastAdapter(requireContext(), filteredPodcastsList) { podcast ->
            // Handle item click
            val intent = Intent(requireContext(), PodcastPlayerActivity::class.java)
            intent.putExtra("PODCAST_ID", podcast.id) // Pass the podcast ID
            startActivity(intent)
        }

        searchResultsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        searchResultsRecyclerView.adapter = podcastAdapter

        // Load podcasts from Firebase
        loadPodcasts()

        // Search input listener
        searchInput.setOnEditorActionListener { _, _, _ ->
            filterPodcasts(searchInput.text.toString())
            true
        }

        val cardExplore: CardView = view.findViewById(R.id.card_explore)
        val cardBrowse: CardView = view.findViewById(R.id.card_browse)

        cardExplore.setOnClickListener {
            // Navigate to Explore Users Tab
            (activity as? MainActivity)?.setCurrentTab(2) // Adjust index for Explore tab
        }

        cardBrowse.setOnClickListener {
            // Navigate to Browse Podcasts Tab
            (activity as? MainActivity)?.setCurrentTab(1) // Adjust index for Browse tab
        }

        return view
    }

    private fun loadPodcasts() {
        val database = FirebaseDatabase.getInstance().getReference("podcasts")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                podcastsList.clear()
                for (podcastSnapshot in snapshot.children) {
                    val podcast = podcastSnapshot.getValue(Podcast::class.java)
                    if (podcast != null) {
                        podcastsList.add(podcast)
                    }
                }
                filteredPodcastsList.clear()
                filteredPodcastsList.addAll(podcastsList) // Show all podcasts initially
                podcastAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load podcasts.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterPodcasts(query: String) {
        filteredPodcastsList.clear()
        if (query.isEmpty()) {
            filteredPodcastsList.addAll(podcastsList) // Show all if the query is empty
        } else {
            for (podcast in podcastsList) {
                if (podcast.title?.contains(query, ignoreCase = true) == true) {
                    filteredPodcastsList.add(podcast) // Add matching podcasts
                }
            }
        }
        podcastAdapter.notifyDataSetChanged() // Update RecyclerView
    }
}
