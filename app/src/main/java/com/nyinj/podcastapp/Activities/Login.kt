package com.nyinj.podcastapp.Activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.nyinj.podcastapp.R

class Login : AppCompatActivity() {
    private val emailPattern = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$")
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        val logemail: EditText = findViewById(R.id.logemail)
        val logpassword: EditText = findViewById(R.id.logPassword)
        val loginbtn: Button = findViewById(R.id.loginbtn)
        val loginProgressbar: ProgressBar = findViewById(R.id.loginprogressBar)

        val createacc: TextView = findViewById(R.id.createacc)

        createacc.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        loginbtn.setOnClickListener {
            loginProgressbar.visibility = View.VISIBLE
            performLogin(logemail.text.toString(), logpassword.text.toString(), loginProgressbar)
        }

        logpassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loginProgressbar.visibility = View.VISIBLE
                performLogin(logemail.text.toString(), logpassword.text.toString(), loginProgressbar)
                true
            } else {
                false
            }
        }
    }

    private fun performLogin(email: String, password: String, progressBar: ProgressBar) {
        if (email.isEmpty() || password.isEmpty()) {
            if (email.isEmpty()) {
                findViewById<EditText>(R.id.logemail).error = "Enter your email address"
            }
            if (password.isEmpty()) {
                findViewById<EditText>(R.id.logPassword).error = "Enter your password"
            }
            progressBar.visibility = View.GONE
            Toast.makeText(this, "Enter valid details", Toast.LENGTH_SHORT).show()
        } else if (!email.matches(emailPattern)) {
            progressBar.visibility = View.GONE
            findViewById<EditText>(R.id.logemail).error = "Enter valid Email"
            Toast.makeText(this, "Enter valid Email", Toast.LENGTH_SHORT).show()
        } else if (password.length < 6) {
            progressBar.visibility = View.GONE
            findViewById<EditText>(R.id.logPassword).error = "Password too Short"
            Toast.makeText(this, "Enter a longer password", Toast.LENGTH_SHORT).show()
        } else {
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                progressBar.visibility = View.GONE
                if (it.isSuccessful) {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK}
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Something went wrong, try again", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
