package com.nyinj.podcastapp.DataClass

data class Podcast(
    val title: String = "",
    val description: String = "",
    val audioUrl: String = "",
    val uploaderName: String = "",
    val uploaderId: String = "",
    val timestamp: Long = 0L
)