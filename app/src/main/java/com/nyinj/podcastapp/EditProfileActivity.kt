package com.nyinj.podcastapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val editUsername: EditText = findViewById(R.id.editUsername)
        val editDescription: EditText = findViewById(R.id.editDescription)
        val saveButton: Button = findViewById(R.id.saveButton)
        val progressBar: ProgressBar = findViewById(R.id.editProgressBar)

        // Load the current user's profile data
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userRef = database.reference.child("users").child(userId)
            userRef.get().addOnSuccessListener { snapshot ->
                val currentUsername = snapshot.child("name").getValue(String::class.java)
                val currentDescription = snapshot.child("description").getValue(String::class.java)

                editUsername.setText(currentUsername)
                editDescription.setText(currentDescription)
            }
        }

        saveButton.setOnClickListener {
            val newUsername = editUsername.text.toString()
            val newDescription = editDescription.text.toString()

            progressBar.visibility = ProgressBar.VISIBLE

            if (userId != null) {
                val userRef = database.reference.child("users").child(userId)

                // Update the username and description in Firebase
                val updates = mapOf(
                    "name" to newUsername,
                    "description" to newDescription
                )

                userRef.updateChildren(updates).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                        finish()  // Close the activity after successful update
                    } else {
                        Toast.makeText(this, "Update failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                    progressBar.visibility = ProgressBar.GONE
                }
            }
        }
    }
}