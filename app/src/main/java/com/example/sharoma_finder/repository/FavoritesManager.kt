package com.example.sharoma_finder.repository

import android.content.Context
import android.util.Log
import androidx.core.content.edit

class FavoritesManager(context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences("sharoma_favorites", Context.MODE_PRIVATE)

    fun getFavorites(): Set<String> {
        val favorites = sharedPreferences.getStringSet("favorite_ids", emptySet()) ?: emptySet()
        return favorites
    }

    fun addFavorite(uniqueKey: String) {
        val currentFavorites = getFavorites().toMutableSet()
        currentFavorites.add(uniqueKey)
        sharedPreferences.edit { putStringSet("favorite_ids", currentFavorites) }
    }

    fun removeFavorite(uniqueKey: String) {
        val currentFavorites = getFavorites().toMutableSet()
        currentFavorites.remove(uniqueKey)
        sharedPreferences.edit { putStringSet("favorite_ids", currentFavorites) }
    }

    fun isFavorite(uniqueKey: String): Boolean {
        val isFav = getFavorites().contains(uniqueKey)
        return isFav
    }
}