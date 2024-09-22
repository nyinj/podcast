package com.nyinj.podcastapp.Adapters

import com.nyinj.podcastapp.DataClass.Podcast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nyinj.podcastapp.R

class PodcastAdapter(
    private val podcasts: List<Podcast>,
    private val onPlayButtonClick: (Podcast) -> Unit // Callback for handling play button click
) : RecyclerView.Adapter<PodcastAdapter.PodcastViewHolder>() {

    class PodcastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.podcast_title)
        val description: TextView = itemView.findViewById(R.id.podcastDescription)
        val playButton: ImageButton = itemView.findViewById(R.id.play_button) // Play button reference
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodcastViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_podcast, parent, false)
        return PodcastViewHolder(view)
    }

    override fun onBindViewHolder(holder: PodcastViewHolder, position: Int) {
        val podcast = podcasts[position]
        holder.title.text = podcast.title
        holder.description.text = podcast.description

        // Handle play button click
        holder.playButton.setOnClickListener {
            onPlayButtonClick(podcast) // Trigger the callback with the clicked podcast
        }
    }

    override fun getItemCount(): Int {
        return podcasts.size
    }
}