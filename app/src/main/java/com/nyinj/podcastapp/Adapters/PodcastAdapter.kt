package com.nyinj.podcastapp.Adapters

import com.nyinj.podcastapp.DataClass.Podcast
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nyinj.podcastapp.R
import com.nyinj.podcastapp.Services.MediaPlayerService
import com.bumptech.glide.Glide
import org.w3c.dom.Text

class PodcastAdapter(
    private val context: Context, // Pass context to the adapter
    private val podcasts: List<Podcast>,
    private val onItemClick: (Podcast) -> Unit // Callback for item click
) : RecyclerView.Adapter<PodcastAdapter.PodcastViewHolder>() {

    class PodcastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val uploader: TextView = itemView.findViewById(R.id.podcastuploader)
        val title: TextView = itemView.findViewById(R.id.podcast_title)
        val description: TextView = itemView.findViewById(R.id.podcastDescription)
        val playButton: ImageButton = itemView.findViewById(R.id.play_button)
        val podcastCoverImage: ImageView = itemView.findViewById(R.id.podcast_cover_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodcastViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_podcast, parent, false)
        return PodcastViewHolder(view)
    }

    override fun onBindViewHolder(holder: PodcastViewHolder, position: Int) {
        val podcast = podcasts[position]
        Log.d("PodcastAdapter", "Binding View for podcast: ${podcast.title}")

        holder.title.text = podcast.title
        holder.uploader.text = podcast.uploaderName ?: "Unknown Uploader" // Handle null case
        holder.description.text = podcast.description ?: "No Description Available" // Handle null case

        Log.d("PodcastCoverURL", "Cover URL: ${podcast.coverUrl}")

        // Check if coverUrl is not null
        if (!podcast.coverUrl.isNullOrEmpty()) {
            val uri = Uri.parse(podcast.coverUrl)
            Glide.with(context)
                .load(uri)
                .placeholder(R.drawable.unknownpodcast)
                .error(R.drawable.unknownpodcast)
                .into(holder.podcastCoverImage)
        } else {
            holder.podcastCoverImage.setImageResource(R.drawable.unknownpodcast) // Use placeholder
        }

        // Handle play button click
        holder.playButton.setOnClickListener {
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
