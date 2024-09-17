package com.nyinj.podcastapp

import UserAdapter
import Users
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BrowseFragment : Fragment() {

    private lateinit var database: FirebaseDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val users = mutableListOf<Users>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = FirebaseDatabase.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_browse, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        userAdapter = UserAdapter(users) { user ->
            followUser(user)
        }
        recyclerView.adapter = userAdapter

        fetchUsers()
    }

    private fun fetchUsers() {
        val usersRef = database.reference.child("users")
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                users.clear()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(Users::class.java)
                    if (user != null) {
                        Log.d("BrowseFragment", "Fetched user: $user")
                        users.add(user)
                    } else {
                        Log.w("BrowseFragment", "User data is null for snapshot: $userSnapshot")
                    }
                }
                userAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error fetching users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun followUser(user: Users) {
        // For example, you might want to update the user's follow status in the database
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val userRef = database.reference.child("users").child(userId ?: "")

        userRef.child("followed_users").child(user.uid ?: "").setValue(!user.isFollowed)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Followed ${user.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error following user", Toast.LENGTH_SHORT).show()
                }
            }
    }
}