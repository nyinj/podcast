package com.nyinj.podcastapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import com.google.firebase.auth.FirebaseAuth

class YouFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request to display a menu in the fragment
        setHasOptionsMenu(true)
        auth = FirebaseAuth.getInstance()  // Initialize FirebaseAuth
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
            // Show the menu when the settings button is clicked
            showSettingsMenu()
        }
    }

    private fun showSettingsMenu() {
        val popupMenu = PopupMenu(requireContext(), requireView().findViewById(R.id.settings_btn))
        popupMenu.menuInflater.inflate(R.menu.settings_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
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

    private fun navigateToLoginScreen() {
        val intent = Intent(requireContext(), Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish() // Optional: Finish the current activity to remove it from the stack
    }
}
