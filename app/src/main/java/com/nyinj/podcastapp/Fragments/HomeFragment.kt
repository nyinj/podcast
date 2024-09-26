package com.nyinj.podcastapp.Fragments

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

    private lateinit var recyclerView: RecyclerView
    private lateinit var podcastAdapter: PodcastAdapter
    private var recentlyPlayedPodcasts: List<Podcast> = listOf() // Initialize with an empty list

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewRecentlyPlayedPodcasts)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val cardExplore: CardView = view.findViewById(R.id.card_explore)
        val cardBrowse: CardView = view.findViewById(R.id.card_browse)

        // Fetch recently played podcasts
        fetchRecentlyPlayedPodcastsFromFirebase()
        cardExplore.setOnClickListener {
            // Navigate to Explore Users Tab
            (activity as? MainActivity)?.setCurrentTab(1) // Adjust index for Explore tab
        }

        cardBrowse.setOnClickListener {
            // Navigate to Browse Podcasts Tab
            (activity as? MainActivity)?.setCurrentTab(2) // Adjust index for Browse tab
        }

        return view
    }

    private fun fetchRecentlyPlayedPodcastsFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val database = FirebaseDatabase.getInstance().reference

        database.child("users").child(userId!!).child("recentlyPlayed").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                recentlyPlayedPodcasts = snapshot.children.mapNotNull { it.getValue(Podcast::class.java) }
                    .sortedByDescending { it.timestamp }

                podcastAdapter = PodcastAdapter(recentlyPlayedPodcasts,
                    { podcast ->
                        val intent = Intent(requireContext(), PodcastPlayerActivity::class.java).apply {
                            putExtra("AUDIO_URL", podcast.audioUrl)
                            putExtra("PODCAST_TITLE", podcast.title)
                        }
                        startActivity(intent)
                    }
                )
                recyclerView.adapter = podcastAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors
                Toast.makeText(requireContext(), "Error fetching data", Toast.LENGTH_SHORT).show()
            }
        })
    }
    fun saveRecentlyPlayedPodcastToFirebase(podcast: Podcast) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val database = FirebaseDatabase.getInstance().reference

        // Add the podcast to the user's recently played podcasts
        database.child("users").child(userId!!).child("recentlyPlayed").child(podcast.title).setValue(podcast)
    }
}