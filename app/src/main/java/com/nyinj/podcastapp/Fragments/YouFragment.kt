package com.nyinj.podcastapp.Fragments

import android.annotation.SuppressLint
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
    private var coverUrl: String? = null
    private var coverUri: Uri? = null
    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>
    private lateinit var profileProgressBar: ProgressBar
    private var profileImageUri: Uri? = null
    private lateinit var uploadCoverButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        imagePickerLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {
                    coverUri = uri
                    Log.d("ImagePicker", "Cover image selected: $coverUri")
                    Toast.makeText(requireContext(), "Cover image selected!", Toast.LENGTH_SHORT)
                        .show()
                }
            }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_you, container, false)

        userRecyclerView = view.findViewById(R.id.userRecyclerView)
        userRecyclerView.layoutManager = LinearLayoutManager(requireContext())


        UserPodcastAdapter = UserPodcastAdapter(requireContext(), userPodcastList,
            { podcast ->
                val intent = Intent(requireContext(), PodcastPlayerActivity::class.java)
                intent.putExtra("AUDIO_URL", podcast.audioUrl)
                intent.putExtra("PODCAST_ID", podcast.id)
                startActivity(intent)
            },
            { podcast -> promptForPodcastEdit(podcast) },
            { podcast, position -> deletePodcast(podcast, position) }
        )

        userRecyclerView.adapter = UserPodcastAdapter
        loadUserPodcasts()

        return view
    }

    private fun deletePodcast(podcast: Podcast, position: Int) {
        val databaseRef = podcast.id?.let {
            FirebaseDatabase.getInstance().getReference("podcasts").child(it)
        }

        if (databaseRef == null) {
            Toast.makeText(requireContext(), "Invalid podcast reference", Toast.LENGTH_SHORT).show()
            return
        }

        databaseRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                UserPodcastAdapter.removePodcastAt(position)
                Toast.makeText(requireContext(), "Podcast deleted successfully", Toast.LENGTH_SHORT)
                    .show()

                podcast.audioUrl?.let { audioUrl ->
                    val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(audioUrl)
                    storageRef.delete().addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            Toast.makeText(
                                requireContext(),
                                "Audio file deleted.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Log.e(
                                "DeletePodcast",
                                "Failed to delete audio file: ${deleteTask.exception?.message}"
                            )
                            Toast.makeText(
                                requireContext(),
                                "Failed to delete audio file.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } ?: run {
                    Log.e("DeletePodcast", "Audio URL is null, skipping deletion.")
                }
            } else {
                Log.e("DeletePodcast", "Failed to delete podcast: ${task.exception?.message}")
                Toast.makeText(requireContext(), "Failed to delete podcast", Toast.LENGTH_SHORT)
                    .show()
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
                    val podcastRef = podcast.id?.let {
                        FirebaseDatabase.getInstance().getReference("podcasts").child(it)
                    }
                    podcastRef?.child("title")?.setValue(newTitle)
                    podcastRef?.child("description")?.setValue(newDescription)
                    Toast.makeText(requireContext(), "Podcast updated!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Title and description cannot be empty.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun loadUserPodcasts() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("LoadPodcasts", "Current User ID: $currentUserId")
        val userPodcastsRef = FirebaseDatabase.getInstance().getReference("podcasts")

        userPodcastsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("LoadPodcasts", "Data snapshot received: ${snapshot.childrenCount}")
                userPodcastList.clear()
                userPodcastCount = 0
                for (podcastSnapshot in snapshot.children) {
                    val podcast = podcastSnapshot.getValue(Podcast::class.java)
                    if (podcast != null && podcast.uploaderId == currentUserId) {
                        userPodcastList.add(podcast)
                        userPodcastCount++
                        Log.d("LoadPodcasts", "Podcast added: ${podcast.title}")
                    }
                }
                UserPodcastAdapter.notifyDataSetChanged()
                val podcastCountTextView = view?.findViewById<TextView>(R.id.podcast_count)
                podcastCountTextView?.text = userPodcastCount.toString()
                Log.d("LoadPodcasts", "Total podcasts: $userPodcastCount")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LoadPodcasts", "Error loading podcasts: ${error.message}")
            }
        })
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressBar = view.findViewById<ProgressBar>(R.id.profile_progress_bar)
        val profileContent = view.findViewById<LinearLayout>(R.id.profile_content)
        val settingsButton = view.findViewById<ImageView>(R.id.settings_btn)

        profileContent.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        settingsButton.setOnClickListener {
            showSettingsMenu()
        }

        val usernameTextView = view.findViewById<TextView>(R.id.username)
        val descriptionTextView = view.findViewById<TextView>(R.id.description)
        val followersTextView = view.findViewById<TextView>(R.id.followers_count)
        val followingTextView = view.findViewById<TextView>(R.id.following_count)
        val profileImageView = view.findViewById<ImageView>(R.id.profile_pic)

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
                        // Load the profile image
                        val profileImageUrl =
                            snapshot.child("profileImageUrl").getValue(String::class.java)
                        if (!profileImageUrl.isNullOrEmpty()) {
                            Glide.with(this@YouFragment)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_default_profile) // Optional placeholder
                                .into(profileImageView)
                        } else {
                            profileImageView.setImageResource(R.drawable.ic_default_profile) // Default image
                        }

                        progressBar.visibility = View.GONE
                        profileContent.visibility = View.VISIBLE
                    } else {
                        usernameTextView.text = "Unknown User"
                        progressBar.visibility = View.GONE
                        profileContent.visibility = View.VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error loading user data", Toast.LENGTH_SHORT)
                        .show()
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
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "audio/*"
            startActivityForResult(intent, REQUEST_CODE_AUDIO)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_AUDIO -> {
                    val audioUri: Uri? = data?.data
                    if (audioUri != null) {
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
        }
    }

    private fun promptForPodcastDetails(audioUri: Uri, userName: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_podcast, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.podcastTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.podcastDescription)
        val chooseCoverButton = dialogView.findViewById<Button>(R.id.btnChooseCover)
        val coverImagePreview = dialogView.findViewById<ImageView>(R.id.coverImagePreview) // Get from dialogView

        chooseCoverButton.setOnClickListener {
            // Launch the image picker for cover image
            imagePickerLauncher.launch("image/*")
        }

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Podcast Information")
            .setView(dialogView)
            .setPositiveButton("Upload") { _, _ ->
                val podcastTitle = titleInput.text.toString()
                val podcastDescription = descriptionInput.text.toString()

                if (podcastTitle.isNotBlank() && podcastDescription.isNotBlank()) {
                    uploadPodcast(audioUri, userName, podcastTitle, podcastDescription, coverUri)
                } else {
                    Toast.makeText(requireContext(), "Title and description cannot be empty.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }



    private fun uploadPodcast(
        audioUri: Uri,
        userName: String,
        podcastTitle: String,
        podcastDescription: String,
        coverUri: Uri? // Ensure this is Uri
    ) {
        val storageRef = storage.reference.child("podcasts/${System.currentTimeMillis()}_${audioUri.lastPathSegment}")
        val coverStorageRef = coverUri?.let {
            storage.reference.child("podcasts/covers/${System.currentTimeMillis()}_${coverUri.lastPathSegment}")
        }

        // Show a loading dialog while uploading
        val loadingDialog = showLoadingDialog(requireContext())

        storageRef.putFile(audioUri).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Get the audio download URL
                storageRef.downloadUrl.addOnSuccessListener { audioUrl ->
                    // If there is a cover image, upload it
                    if (coverUri != null && coverStorageRef != null) {
                        coverStorageRef.putFile(coverUri).addOnCompleteListener { coverTask ->
                            if (coverTask.isSuccessful) {
                                // Get the cover image download URL
                                coverStorageRef.downloadUrl.addOnSuccessListener { coverUrl ->
                                    savePodcastToDatabase(audioUrl.toString(), userName, podcastTitle, podcastDescription, coverUrl.toString())
                                    loadingDialog.dismiss()
                                }
                            } else {
                                loadingDialog.dismiss()
                                Log.e("UploadPodcast", "Failed to upload cover image: ${coverTask.exception?.message}")
                                Toast.makeText(requireContext(), "Failed to upload cover image", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        // No cover image, just save the podcast with audio URL
                        savePodcastToDatabase(audioUrl.toString(), userName, podcastTitle, podcastDescription, null)
                        loadingDialog.dismiss()
                    }
                }
            } else {
                loadingDialog.dismiss()
                Log.e("UploadPodcast", "Failed to upload podcast: ${task.exception?.message}")
                Toast.makeText(requireContext(), "Failed to upload podcast", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePodcastToDatabase(
        audioUrl: String,
        userName: String,
        podcastTitle: String,
        podcastDescription: String,
        coverUrl: String?
    ) {
        val podcastId = database.reference.child("podcasts").push().key
        val podcast = Podcast(
            id = podcastId,
            title = podcastTitle,
            description = podcastDescription,
            audioUrl = audioUrl,
            coverUrl = coverUrl,
            uploaderName = userName,
            uploaderId = auth.currentUser?.uid,
            timestamp = System.currentTimeMillis()
        )

        podcastId?.let {
            database.reference.child("podcasts").child(it).setValue(podcast).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Podcast uploaded successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("SavePodcast", "Failed to save podcast to database: ${task.exception?.message}")
                    Toast.makeText(requireContext(), "Failed to save podcast", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun savePodcastMetadata(audioUrl: String, userName: String, podcastTitle: String, podcastDescription: String, userId: String?, coverUrl: String?) {
        Log.d("SavePodcastMetadata", "Cover URL: $coverUrl")
        val podcastRef = FirebaseDatabase.getInstance().getReference("podcasts").push()
        val podcastId = podcastRef.key

        if (podcastId != null) {
            val podcast = Podcast(
                id = podcastId,
                title = podcastTitle,
                description = podcastDescription,
                audioUrl = audioUrl,
                uploaderName = userName,
                uploaderId = userId ?: "",
                timestamp = System.currentTimeMillis(),
                coverUrl = coverUrl
            )

            podcastRef.setValue(podcast).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("SavePodcastMetadata", "Podcast uploaded successfully: $podcastId")
                    Toast.makeText(requireContext(), "Podcast uploaded successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("SavePodcastMetadata", "Failed to upload podcast metadata: ${task.exception?.message}")
                    Toast.makeText(requireContext(), "Failed to upload podcast metadata.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.e("SavePodcastMetadata", "Error generating podcast ID.")
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
        auth.signOut()
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
        requireActivity().finish()
    }
}
