package com.example.sharoma_finder.repository

import android.content.Context
import androidx.core.content.edit

class FavoritesManager(context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences("sharoma_favorites", Context.MODE_PRIVATE)

    fun getFavorites(): Set<String> {
        return sharedPreferences.getStringSet("favorite_ids", emptySet()) ?: emptySet()
    }

    fun addFavorite(id: Int) {
        val currentFavorites = getFavorites().toMutableSet()
        currentFavorites.add(id.toString())
        sharedPreferences.edit { putStringSet("favorite_ids", currentFavorites) }
    }

    fun removeFavorite(id: Int) {
        val currentFavorites = getFavorites().toMutableSet()
        currentFavorites.remove(id.toString())
        sharedPreferences.edit { putStringSet("favorite_ids", currentFavorites) }
    }
}