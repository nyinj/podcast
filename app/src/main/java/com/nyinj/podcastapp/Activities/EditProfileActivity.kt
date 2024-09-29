package com.nyinj.podcastapp.Activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.nyinj.podcastapp.R

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference

    private lateinit var profileImageView: ImageView
    private lateinit var editName: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPhone: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var editDescription: EditText // Updated

    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        profileImageView = findViewById(R.id.profile_image)
        editName = findViewById(R.id.edit_name)
        editEmail = findViewById(R.id.edit_email)
        editPhone = findViewById(R.id.edit_phone)
        progressBar = findViewById(R.id.editProgressBar)
        editDescription = findViewById(R.id.edit_description)

        val addPictureButton: Button = findViewById(R.id.add_picture_button)
        val saveButton: Button = findViewById(R.id.save_button)

        // Load the current user's profile data
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userRef = database.reference.child("users").child(userId)
            userRef.get().addOnSuccessListener { snapshot ->
                editName.setText(snapshot.child("name").getValue(String::class.java))
                editEmail.setText(snapshot.child("email").getValue(String::class.java))
                editPhone.setText(snapshot.child("phone").getValue(String::class.java))
                editDescription.setText(snapshot.child("description").getValue(String::class.java)) // Updated
                // Load the profile image if available
                val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                if (profileImageUrl != null) {
                    // Use Glide to load the image
                    Glide.with(this)
                        .load(profileImageUrl)
                        .placeholder(R.drawable.profile) // Placeholder image
                        .error(R.drawable.profile) // Error image
                        .into(profileImageView)
                } else {
                    // Load a default image if no URL is available
                    profileImageView.setImageResource(R.drawable.profile)
                }
            }
        }

        // Set up the add picture button
        addPictureButton.setOnClickListener {
            openImageChooser()
        }

        saveButton.setOnClickListener {
            val newName = editName.text.toString()
            val newEmail = editEmail.text.toString()
            val newPhone = editPhone.text.toString()
            val newDescription = editDescription.text.toString() // Updated

            progressBar.visibility = ProgressBar.VISIBLE

            if (userId != null) {
                val userRef = database.reference.child("users").child(userId)

                // If image is selected, upload it to Firebase Storage
                if (imageUri != null) {
                    val fileRef = storageRef.child("profile_images/${userId}.jpg")
                    fileRef.putFile(imageUri!!).addOnSuccessListener { taskSnapshot ->
                        fileRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                            val updates = mapOf(
                                "name" to newName,
                                "email" to newEmail,
                                "phone" to newPhone,
                                "profileImageUrl" to downloadUrl.toString(), // Save the image URL
                                "description" to newDescription // Updated
                            )

                            userRef.updateChildren(updates).addOnCompleteListener { updateTask ->
                                handleUpdateResponse(updateTask)
                            }
                        }
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = ProgressBar.GONE
                    }
                } else {
                    // No image selected, just update text fields
                    val updates = mapOf(
                        "name" to newName,
                        "email" to newEmail,
                        "phone" to newPhone,
                        "description" to newDescription // Updated
                    )

                    userRef.updateChildren(updates).addOnCompleteListener { updateTask ->
                        handleUpdateResponse(updateTask)
                    }
                }
            }
        }
    }

    private fun handleUpdateResponse(task: Task<Void>) {
        if (task.isSuccessful) {
            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            finish()  // Close the activity after successful update
        } else {
            Toast.makeText(this, "Update failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
        }
        progressBar.visibility = ProgressBar.GONE
    }

    private fun openImageChooser() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            profileImageView.setImageURI(imageUri) // Display the selected image
        }
    }
}
