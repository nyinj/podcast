package com.nyinj.podcastapp.utils

import android.content.Context

object FavoriteUtils {

    fun addFavorite(context: Context, podcastId: String) {
        val sharedPref = context.getSharedPreferences("Favorites", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val favorites = sharedPref.getStringSet("favorite_podcasts", mutableSetOf()) ?: mutableSetOf()
        favorites.add(podcastId)
        editor.putStringSet("favorite_podcasts", favorites)
        editor.apply()
    }

    fun removeFavorite(context: Context, podcastId: String) {
        val sharedPref = context.getSharedPreferences("Favorites", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val favorites = sharedPref.getStringSet("favorite_podcasts", mutableSetOf()) ?: mutableSetOf()
        favorites.remove(podcastId)
        editor.putStringSet("favorite_podcasts", favorites)
        editor.apply()
    }

    fun isFavorite(context: Context, podcastId: String): Boolean {
        val sharedPref = context.getSharedPreferences("Favorites", Context.MODE_PRIVATE)
        val favorites = sharedPref.getStringSet("favorite_podcasts", mutableSetOf())
        return favorites?.contains(podcastId) ?: false
    }
}
