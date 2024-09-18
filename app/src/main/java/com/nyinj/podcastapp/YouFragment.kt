package com.nyinj.podcastapp

import Users
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class YouFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        auth = FirebaseAuth.getInstance()  // Initialize FirebaseAuth
        database = FirebaseDatabase.getInstance()  // Initialize Firebase Database
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_you, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressBar = view.findViewById<ProgressBar>(R.id.profile_progress_bar)
        val profileContent = view.findViewById<LinearLayout>(R.id.profile_content)
        val settingsButton = view.findViewById<ImageView>(R.id.settings_btn)

        // Hide profile content initially, show progress bar
        profileContent.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        settingsButton.setOnClickListener {
            showSettingsMenu()  // Ensure this function gets called
        }

        val usernameTextView = view.findViewById<TextView>(R.id.username)
        val descriptionTextView = view.findViewById<TextView>(R.id.description)
        val followersTextView = view.findViewById<TextView>(R.id.followers_count)
        val followingTextView = view.findViewById<TextView>(R.id.following_count)

        val userId = auth.currentUser?.uid
        if (userId != null) {
            databaseRef = database.reference.child("users").child(userId)

            databaseRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(Users::class.java)
                    if (user != null) {
                        usernameTextView.text = user.name
                        descriptionTextView.text = user.description

                        val followersCount = snapshot.child("followersCount").getValue(Int::class.java) ?: 0
                        val followingCount = snapshot.child("followingCount").getValue(Int::class.java) ?: 0

                        followersTextView.text = "$followersCount"
                        followingTextView.text = "$followingCount"

                        // Hide progress bar, show profile content once data is loaded
                        progressBar.visibility = View.GONE
                        profileContent.visibility = View.VISIBLE
                    } else {
                        usernameTextView.text = "Unknown User"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle potential errors
                    Toast.makeText(requireContext(), "Error loading user data", Toast.LENGTH_SHORT).show()

                    // Hide progress bar if there is an error, but still show content
                    progressBar.visibility = View.GONE
                    profileContent.visibility = View.VISIBLE
                }
            })
        } else {
            usernameTextView.text = "Not logged in"
            progressBar.visibility = View.GONE
            profileContent.visibility = View.VISIBLE
        }
    }

    private fun showSettingsMenu() {
        val popupMenu = PopupMenu(requireContext(), requireView().findViewById(R.id.settings_btn))
        popupMenu.menuInflater.inflate(R.menu.settings_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.edit_profile -> {
                    navigateToEditProfile()
                    true
                }
                R.id.logout -> {
                    performLogout()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun performLogout() {
        auth.signOut()  // Sign out using Firebase Authentication
        navigateToLoginScreen()
        Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToEditProfile() {
        val intent = Intent(requireContext(), EditProfileActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToLoginScreen() {
        val intent = Intent(requireContext(), Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish() // Optional: Finish the current activity to remove it from the stack
    }
}
