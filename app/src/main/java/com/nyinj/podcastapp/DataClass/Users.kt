package com.nyinj.podcastapp.DataClass

data class Users(
    var name: String? = null,
    var email: String? = null,
    var uid: String? = null,
    val description: String = "",
    val profileImageUrl: String = "",
    var isFollowed: Boolean = false,
    var followersCount: Int = 0,
    var followingCount: Int = 0,
    val favorites: MutableList<String> = mutableListOf()
)