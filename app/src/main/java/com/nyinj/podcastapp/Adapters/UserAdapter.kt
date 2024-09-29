package com.nyinj.podcastapp.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nyinj.podcastapp.DataClass.Users
import com.nyinj.podcastapp.R

class UserAdapter(
    private val users: List<Users>,
    private val onFollowClick: (Users) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImageView: ImageView = itemView.findViewById(R.id.user_profile_image)
        val nameTextView: TextView = view.findViewById(R.id.user_name)
        val descriptionTextView: TextView = view.findViewById(R.id.user_description)
        val followButton: Button = view.findViewById(R.id.follow_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {

        val user = users[position]
        holder.nameTextView.text = user.name
        holder.descriptionTextView.text = user.description
        holder.followButton.text = if (user.isFollowed) "Unfollow" else "Follow"
        holder.followButton.setOnClickListener {
            onFollowClick(user)
        }
        // Load the user's profile image
        if (user.profileImageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.ic_default_profile) // Placeholder image
                .error(R.drawable.ic_default_profile) // Error image
                .into(holder.profileImageView)
        } else {
            holder.profileImageView.setImageResource(R.drawable.ic_default_profile) // Default image
        }
    }


    override fun getItemCount() = users.size
}