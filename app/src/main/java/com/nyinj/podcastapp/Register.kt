package com.nyinj.podcastapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Register : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val emailPattern ="[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.signup)

        val signUsername: EditText = findViewById(R.id.signUsername)
        val signemail: EditText = findViewById(R.id.signemail)
        val signpassword: EditText = findViewById(R.id.signPassword)
        val createaccbtn : Button = findViewById(R.id.createaccbtn)







        val loginacc : TextView =findViewById(R.id.loginacc)

        loginacc.setOnClickListener {
            val intent = Intent(this, Login::class.java )
            startActivity(intent)
        }

        createaccbtn.setOnClickListener{
            val name = signUsername.text.toString()
            val email = signemail.text.toString()
            val password = signpassword.text.toString()

            if(name.isEmpty() || email.isEmpty() || password.isEmpty()){
                if(name.isEmpty()){
                    signUsername.error = "Enter your Name"
                }
                if(email.isEmpty()){
                    signemail.error = "Enter your Email"
                }
                if(password.isEmpty()){
                    signpassword.error = "Enter your Password"
                }
                Toast.makeText(this, "Enter valid details", Toast.LENGTH_SHORT).show()
            }else if (!email.matches(emailPattern.toRegex())){
                signemail.error = "Enter valid Email"
                Toast.makeText(this, "Enter valif Email", Toast.LENGTH_SHORT).show()
            }else if (password.length <6){
                signpassword.error= "Password too Short"
                Toast.makeText(this, "Enter a longer password", Toast.LENGTH_SHORT).show()
            }else{
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener{
                    if(it.isSuccessful){
                        val databaseRef = database.reference.child("users").child(auth.currentUser!!.uid)
                        val users:Users = Users(name,email, auth.currentUser!!.uid)
                        
                        databaseRef.setValue(users).addOnCompleteListener{
                            if(it.isSuccessful){
                                val intent = Intent (this, Login::class.java)
                                startActivity(intent)
                            }else{
                                Toast.makeText(this, "Something went wrong, try again", Toast.LENGTH_SHORT).show()
                            }
                        }

                    }else{
                        Toast.makeText(this, "Something went wrong, try again", Toast.LENGTH_SHORT).show()

                    }
                }
            }


        }



    }



}

