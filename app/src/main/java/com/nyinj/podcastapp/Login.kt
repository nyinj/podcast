package com.nyinj.podcastapp

import android.content.Intent
import android.os.Bundle
import android.renderscript.ScriptGroup.Binding
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


class Login : AppCompatActivity() {
    private val emailPattern = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$")
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login)

        auth = FirebaseAuth.getInstance()

        val logemail: EditText = findViewById(R.id.logemail)
        val logpassword: EditText = findViewById(R.id.logPassword)
        val logPasswordLayout: TextInputLayout = findViewById(R.id.logPasswordLayout)
        val loginbtn: Button = findViewById(R.id.loginbtn)
        val loginProgressbar: ProgressBar = findViewById(R.id.loginprogressBar)

        val createacc: TextView = findViewById(R.id.createacc)

        createacc.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        loginbtn.setOnClickListener {
            loginProgressbar.visibility = View.VISIBLE

            val email = logemail.text.toString()
            val password = logpassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                if (email.isEmpty()) {
                    logemail.error = "Enter your email address"
                }
                if (password.isEmpty()) {
                    logpassword.error = "Enter your password"
                }
                loginProgressbar.visibility = View.GONE
                Toast.makeText(this, "Enter valid details", Toast.LENGTH_SHORT).show()
            } else if (!email.matches(emailPattern)) {
                loginProgressbar.visibility = View.GONE
                logemail.error = "Enter valid Email"
                Toast.makeText(this, "Enter valid Email", Toast.LENGTH_SHORT).show()
            } else if (password.length < 6) {
                loginProgressbar.visibility = View.GONE
                logpassword.error = "Password too Short"
                Toast.makeText(this, "Enter a longer password", Toast.LENGTH_SHORT).show()
            } else {
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                    loginProgressbar.visibility = View.GONE
                    if (it.isSuccessful) {
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Something went wrong, try again", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}