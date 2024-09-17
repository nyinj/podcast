package com.nyinj.podcastapp

import Users
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ImageView
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

        // Set up the settings button click listener
        view.findViewById<ImageView>(R.id.settings_btn).setOnClickListener {
            showSettingsMenu()
        }

        // Reference to the TextView where the username will be displayed
        val usernameTextView = view.findViewById<TextView>(R.id.username)
        val descriptionTextView = view.findViewById<TextView>(R.id.description)

        // Get the current user ID
        val userId = auth.currentUser?.uid
        if (userId != null) {
            // Reference to the user's data in the Realtime Database
            databaseRef = database.reference.child("users").child(userId)

            // Fetch the username from the Realtime Database
            databaseRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Get the user data as a Users object
                    val user = snapshot.getValue(Users::class.java)
                    if (user != null) {
                        usernameTextView.text = user.name
                        descriptionTextView.text = user.description

                    } else {
                        usernameTextView.text = "Unknown User"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle potential errors
                    Toast.makeText(requireContext(), "Error loading user data", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            usernameTextView.text = "Not logged in"
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