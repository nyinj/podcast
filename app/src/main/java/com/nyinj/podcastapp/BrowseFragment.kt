package com.nyinj.podcastapp

import UserAdapter
import Users
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener

class BrowseFragment : Fragment() {

    private lateinit var database: FirebaseDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var progressBar: ProgressBar
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
        progressBar = view.findViewById(R.id.progress_bar)

        fetchUsers()
    }

    private fun fetchUsers() {
        progressBar.visibility = View.VISIBLE // Show ProgressBar
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val followingRef = database.reference.child("users").child(currentUserId).child("following")
        val usersRef = database.reference.child("users")

        followingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(followingSnapshot: DataSnapshot) {
                val followingSet = mutableSetOf<String>()
                for (followedUser in followingSnapshot.children) {
                    followedUser.key?.let { followingSet.add(it) }
                }

                usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        users.clear()
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(Users::class.java)
                            if (user != null && user.uid != currentUserId && !followingSet.contains(user.uid)) {
                                users.add(user)
                            }
                        }
                        userAdapter.notifyDataSetChanged()
                        progressBar.visibility = View.GONE // Hide ProgressBar
                        recyclerView.visibility = View.VISIBLE // Show RecyclerView
                    }

                    override fun onCancelled(error: DatabaseError) {
                        progressBar.visibility = View.GONE // Hide ProgressBar on error
                        Toast.makeText(context, "Error fetching users: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE // Hide ProgressBar on error
                Toast.makeText(context, "Error fetching followed users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun followUser(user: Users) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userIdToFollow = user.uid ?: return
        val userRef = database.reference.child("users").child(currentUserId)
        val followedUserRef = database.reference.child("users").child(userIdToFollow)

        // Check if the user is already followed or not
        userRef.child("following").child(userIdToFollow).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // If the user is already followed, unfollow them
                    userRef.child("following").child(userIdToFollow).removeValue()
                    followedUserRef.child("followers").child(currentUserId).removeValue()

                    // Decrease follower and following counts
                    userRef.child("followingCount").setValue(ServerValue.increment(-1))
                    followedUserRef.child("followersCount").setValue(ServerValue.increment(-1))

                    user.isFollowed = false
                    Toast.makeText(context, "Unfollowed ${user.name}", Toast.LENGTH_SHORT).show()
                } else {
                    // Follow the user
                    userRef.child("following").child(userIdToFollow).setValue(true)
                    followedUserRef.child("followers").child(currentUserId).setValue(true)

                    // Increase follower and following counts
                    userRef.child("followingCount").setValue(ServerValue.increment(1))
                    followedUserRef.child("followersCount").setValue(ServerValue.increment(1))

                    user.isFollowed = true
                    Toast.makeText(context, "Followed ${user.name}", Toast.LENGTH_SHORT).show()

                    // Remove the followed user from the browse list
                    users.remove(user)
                }

                userAdapter.notifyDataSetChanged() // Update the list to reflect changes
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


}