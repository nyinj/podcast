package com.nyinj.podcastapp.Adapters

import com.nyinj.podcastapp.DataClass.Podcast
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nyinj.podcastapp.R
import com.nyinj.podcastapp.Services.MediaPlayerService

class PodcastAdapter(
    private val context: Context, // Pass context to the adapter
    private val podcasts: List<Podcast>,
    private val onItemClick: (Podcast) -> Unit // Callback for item click
) : RecyclerView.Adapter<PodcastAdapter.PodcastViewHolder>() {

    class PodcastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.podcast_title)
        val description: TextView = itemView.findViewById(R.id.podcastDescription)
        val playButton: ImageButton = itemView.findViewById(R.id.play_button)
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
            // Start the MediaPlayerService directly
            val serviceIntent = Intent(context, MediaPlayerService::class.java).apply {
                putExtra("AUDIO_URL", podcast.audioUrl) // Pass the audio URL
            }
            context.startService(serviceIntent) // Start the media player service
        }

        // Handle item click
        holder.itemView.setOnClickListener {
            onItemClick(podcast) // Trigger the item click callback
        }
    }

    override fun getItemCount(): Int {
        return podcasts.size
    }
}
