package com.nyinj.podcastapp.Fragments

import com.nyinj.podcastapp.DataClass.Podcast
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.nyinj.podcastapp.Activities.EditProfileActivity
import com.nyinj.podcastapp.Activities.Login
import com.nyinj.podcastapp.Activities.PodcastPlayerActivity
import com.nyinj.podcastapp.Adapters.UserPodcastAdapter
import com.nyinj.podcastapp.DataClass.Users
import com.nyinj.podcastapp.R

class YouFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseRef: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private val REQUEST_CODE_AUDIO = 1001
    private lateinit var userRecyclerView: RecyclerView
    private lateinit var UserPodcastAdapter: UserPodcastAdapter
    private val userPodcastList = mutableListOf<Podcast>()
    private var userPodcastCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        auth = FirebaseAuth.getInstance()  // Initialize FirebaseAuth
        database = FirebaseDatabase.getInstance()  // Initialize Firebase Database
        storage = FirebaseStorage.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_you, container, false)

        // Set up RecyclerView
        userRecyclerView = view.findViewById(R.id.userRecyclerView)
        userRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        UserPodcastAdapter = UserPodcastAdapter(requireContext(), userPodcastList,
            { podcast ->
                // Play podcast
                val intent = Intent(requireContext(), PodcastPlayerActivity::class.java)
                intent.putExtra("AUDIO_URL", podcast.audioUrl)
                intent.putExtra("PODCAST_ID", podcast.id)
                startActivity(intent)
            },
            { podcast ->
                // Handle edit action
                promptForPodcastEdit(podcast)
            },
            { podcast, position -> // Updated this line
                // Handle delete action
                deletePodcast(podcast, position) // Pass position to the delete method
            }
        )

        userRecyclerView.adapter = UserPodcastAdapter

        // Fetch data from Firebase for current user
        loadUserPodcasts()

        return view
    }



    private fun deletePodcast(podcast: Podcast, position: Int) {
        val databaseRef = podcast.id?.let {
            FirebaseDatabase.getInstance().getReference("podcasts").child(it)
        }

        // Check if database reference is valid
        if (databaseRef == null) {
            Toast.makeText(requireContext(), "Invalid podcast reference", Toast.LENGTH_SHORT).show()
            return
        }

        // Start deletion from Firebase Database
        databaseRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // On successful deletion from Firebase, remove it from the adapter
                UserPodcastAdapter.removePodcastAt(position)
                Toast.makeText(requireContext(), "Podcast deleted successfully", Toast.LENGTH_SHORT).show()

                // Attempt to delete the audio file from Firebase Storage if the audioUrl is not null
                podcast.audioUrl?.let { audioUrl ->
                    val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(audioUrl)
                    storageRef.delete().addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            Toast.makeText(requireContext(), "Audio file deleted.", Toast.LENGTH_SHORT).show()
                        } else {
                            // Log specific error message
                            Log.e("DeletePodcast", "Failed to delete audio file: ${deleteTask.exception?.message}")
                            Toast.makeText(requireContext(), "Failed to delete audio file.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } ?: run {
                    Log.e("DeletePodcast", "Audio URL is null, skipping deletion.")
                }
            } else {
                // Log specific error message
                Log.e("DeletePodcast", "Failed to delete podcast: ${task.exception?.message}")
                Toast.makeText(requireContext(), "Failed to delete podcast", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun showLoadingDialog(context: Context): Dialog {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_loading)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        dialog.show()
        return dialog
    }



    private fun promptForPodcastEdit(podcast: Podcast) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_podcast, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.podcastTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.podcastDescription)

        titleInput.setText(podcast.title)
        descriptionInput.setText(podcast.description)

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Edit Podcast")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = titleInput.text.toString()
                val newDescription = descriptionInput.text.toString()

                if (newTitle.isNotBlank() && newDescription.isNotBlank()) {
                    // Update the podcast metadata
                    val podcastRef =
                        podcast.id?.let {
                            FirebaseDatabase.getInstance().getReference("podcasts").child(
                                it
                            )
                        }
                    if (podcastRef != null) {
                        podcastRef.child("title").setValue(newTitle)
                    }
                    if (podcastRef != null) {
                        podcastRef.child("description").setValue(newDescription)
                    }
                    Toast.makeText(requireContext(), "Podcast updated!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Title and description cannot be empty.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }


    private fun loadUserPodcasts() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val userPodcastsRef = FirebaseDatabase.getInstance().getReference("podcasts")

        userPodcastsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userPodcastList.clear() // Clear old data
                userPodcastCount = 0 // Reset count for this fetch
                for (podcastSnapshot in snapshot.children) {
                    val podcast = podcastSnapshot.getValue(Podcast::class.java)
                    Log.d("YouFragment", "com.nyinj.podcastapp.DataClass.Podcast: $podcast")
                    if (podcast != null && podcast.uploaderId == currentUserId) {
                        userPodcastList.add(podcast)
                        userPodcastCount++ // Increment count for each podcast
                    }
                }
                Log.d("YouFragment", "User Podcasts: $userPodcastList")
                UserPodcastAdapter.notifyDataSetChanged() // Update RecyclerView

                // Update the podcast count TextView
                val podcastCountTextView = view?.findViewById<TextView>(R.id.podcast_count)
                podcastCountTextView?.text = userPodcastCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
            }
        })
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

        val uploadButton = view.findViewById<Button>(R.id.btnUploadPodcast)
        uploadButton.setOnClickListener {
            // Open file picker for audio files
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "audio/*"
            startActivityForResult(intent, REQUEST_CODE_AUDIO)
        }
    }
    // Handle the result of file picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_AUDIO && resultCode == Activity.RESULT_OK) {
            val audioUri: Uri? = data?.data
            if (audioUri != null) {
                // Fetch userName
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    databaseRef = database.reference.child("users").child(userId)
                    databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val user = snapshot.getValue(Users::class.java)
                            val userName = user?.name ?: "Unknown User"
                            promptForPodcastDetails(audioUri, userName)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(requireContext(), "Error fetching user data.", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }
        }
    }
    // Prompt for podcast title and description
    private fun promptForPodcastDetails(audioUri: Uri, userName: String) {
        // Create a dialog to ask for podcast title and description
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_podcast, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.podcastTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.podcastDescription)

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Podcast Information")
            .setView(dialogView)
            .setPositiveButton("Upload") { _, _ ->
                val podcastTitle = titleInput.text.toString()
                val podcastDescription = descriptionInput.text.toString()

                if (podcastTitle.isNotBlank() && podcastDescription.isNotBlank()) {
                    uploadPodcast(audioUri, userName, podcastTitle, podcastDescription)
                } else {
                    Toast.makeText(requireContext(), "Title and description cannot be empty.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }
    // Function to upload podcast
    private fun uploadPodcast(audioUri: Uri, userName: String, podcastTitle: String, podcastDescription: String) {
        // Show loading dialog
        val progressDialog = showLoadingDialog(requireContext())

        val userId = auth.currentUser?.uid
        val storageRef = storage.reference.child("podcasts/${System.currentTimeMillis()}.mp3")

        // Upload file to Firebase Storage
        storageRef.putFile(audioUri).addOnSuccessListener { taskSnapshot ->
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val audioUrl = uri.toString()
                savePodcastMetadata(audioUrl, userName, podcastTitle, podcastDescription, userId)
                progressDialog.dismiss() // Dismiss dialog on success
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Failed to upload podcast: ${e.message}", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss() // Dismiss dialog on failure
        }
    }


    // Save podcast metadata to Firebase Realtime Database
    private fun savePodcastMetadata(audioUrl: String, userName: String, podcastTitle: String, podcastDescription: String, userId: String?) {
        // Get a reference to the 'podcasts' node and generate a unique ID using 'push()'
        val podcastRef = FirebaseDatabase.getInstance().getReference("podcasts").push()
        val podcastId = podcastRef.key // Get the unique key generated by push()

        if (podcastId != null) {
            // Create a Podcast object and include the generated ID
            val podcast = Podcast(
                id = podcastId, // Set the generated ID
                title = podcastTitle,
                description = podcastDescription,
                audioUrl = audioUrl,
                uploaderName = userName,
                uploaderId = userId ?: "",
                timestamp = System.currentTimeMillis()
            )

            // Save the podcast metadata with the generated ID
            podcastRef.setValue(podcast).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Podcast uploaded successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to upload podcast metadata.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Error generating podcast ID.", Toast.LENGTH_SHORT).show()
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
