package com.nyinj.podcastapp.Adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nyinj.podcastapp.DataClass.Podcast
import com.nyinj.podcastapp.R

class UserPodcastAdapter(
    private val context: Context,
    private val podcastList: MutableList<Podcast>,
    private val onPodcastClick: (Podcast) -> Unit,
    private val onEditClick: (Podcast) -> Unit,    // Callback for edit action
    private val onDeleteClick: (Podcast, Int) -> Unit  // Callback for delete action
) : RecyclerView.Adapter<UserPodcastAdapter.PodcastViewHolder>() {

    class PodcastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val uploader: TextView = itemView.findViewById(R.id.podcastuploader)
        val titleTextView: TextView = itemView.findViewById(R.id.podcast_title)  // Update to correct ID
        val descriptionTextView: TextView = itemView.findViewById(R.id.podcastDescription)
        val overflowMenu: ImageView = itemView.findViewById(R.id.overflow_menu)
        val podcastCoverImage: ImageView = itemView.findViewById(R.id.podcast_cover_image) // Add this line
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodcastViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_podcast, parent, false)
        return PodcastViewHolder(view)
    }

    override fun onBindViewHolder(holder: PodcastViewHolder, position: Int) {
        val podcast = podcastList[position]

        holder.titleTextView.text = podcast.title
        holder.descriptionTextView.text = podcast.description
        holder.uploader.text = podcast.uploaderName ?: "Unknown Uploader" // Handle null case

        holder.itemView.setOnClickListener {
            onPodcastClick(podcast)
        }
        // Load the podcast cover image
        if (!podcast.coverUrl.isNullOrEmpty()) {
            Glide.with(context)
                .load(podcast.coverUrl) // Use the correct field for the cover image
                .placeholder(R.drawable.unknownpodcast) // Placeholder image
                .error(R.drawable.unknownpodcast) // Error image if loading fails
                .into(holder.podcastCoverImage) // Set the image to the ImageView
        } else {
            holder.podcastCoverImage.setImageResource(R.drawable.unknownpodcast) // Handle case where URL is null
        }

        holder.itemView.setOnClickListener {
            onPodcastClick(podcast)
        }


        // Show overflow menu (edit, delete) only for podcasts uploaded by the logged-in user
        holder.overflowMenu.visibility = View.VISIBLE

        holder.overflowMenu.setOnClickListener {
            showPopupMenu(it, podcast, position)
        }
    }

    override fun getItemCount(): Int {
        return podcastList.size
    }

    private fun showPopupMenu(view: View, podcast: Podcast, position: Int) {
        val popup = PopupMenu(context, view)
        popup.menuInflater.inflate(R.menu.podcast_options_menu, popup.menu)
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.edit_podcast -> {
                    onEditClick(podcast)
                    true
                }
                R.id.delete_podcast -> {
                    onDeleteClick(podcast, position)  // Pass position for deletion
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    fun removePodcastAt(position: Int) {
        if (position >= 0 && position < podcastList.size) {
            podcastList.removeAt(position)
            notifyItemRemoved(position)
        } else {
            Log.e("UserPodcastAdapter", "Attempted to remove item at invalid position: $position")
        }
    }

}
