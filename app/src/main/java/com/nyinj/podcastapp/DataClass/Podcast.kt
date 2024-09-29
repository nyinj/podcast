package com.nyinj.podcastapp.DataClass

data class Podcast(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val audioUrl: String? = null,
    val uploaderName: String? = null,
    val uploaderId: String? = null,
    val timestamp: Long? = null,
    val coverUrl: String? = null,
)