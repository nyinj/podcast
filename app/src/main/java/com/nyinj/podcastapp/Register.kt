package com.nyinj.podcastapp

import Users
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Register : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val emailPattern = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.signup)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val signUsername: EditText = findViewById(R.id.signUsername)
        val signemail: EditText = findViewById(R.id.signemail)
        val signPasswordLayout : TextInputLayout =  findViewById(R.id.signPasswordLayout)
        val signpassword: EditText = findViewById(R.id.signPassword)
        val createaccbtn: Button = findViewById(R.id.createaccbtn)
        val loginacc: TextView = findViewById(R.id.loginacc)
        val progressBar: ProgressBar = findViewById(R.id.signupprogressBar)

        loginacc.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        createaccbtn.setOnClickListener {
            val name = signUsername.text.toString()
            val email = signemail.text.toString()
            val password = signpassword.text.toString()

            progressBar.visibility = View.VISIBLE
            signPasswordLayout.isPasswordVisibilityToggleEnabled = true

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                if (name.isEmpty()) {
                    signUsername.error = "Enter your Name"
                }
                if (email.isEmpty()) {
                    signemail.error = "Enter your Email"
                }
                if (password.isEmpty()) {
                    signPasswordLayout.isPasswordVisibilityToggleEnabled = false
                    signpassword.error = "Enter your Password"
                }
                Toast.makeText(this, "Enter valid details", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            } else if (!email.matches(emailPattern)) {
                progressBar.visibility = View.GONE
                signemail.error = "Enter valid Email"
                Toast.makeText(this, "Enter valid Email", Toast.LENGTH_SHORT).show()
            } else if (password.length < 6) {
                signPasswordLayout.isPasswordVisibilityToggleEnabled = false
                progressBar.visibility = View.GONE
                signpassword.error = "Password too Short"
                Toast.makeText(this, "Enter a longer password", Toast.LENGTH_SHORT).show()
            } else {
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        // User successfully registered
                        val databaseRef = database.reference.child("users").child(auth.currentUser!!.uid)
                        val users = Users(name, email, auth.currentUser!!.uid)

                        databaseRef.setValue(users).addOnCompleteListener { dbTask ->
                            if (dbTask.isSuccessful) {
                                // Explicitly sign out user before navigating to Login
                                auth.signOut() // Ensure user is signed out
                                val intent = Intent(this, Login::class.java)
                                startActivity(intent)
                                finish() // Finish the Register activity
                            } else {
                                Toast.makeText(this, "Database Error: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                progressBar.visibility = View.GONE
                            }
                        }
                    } else {
                        Toast.makeText(this, "Authentication Error: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                    }
                }
            }
        }

    }
}


