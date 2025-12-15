package com.example.sharoma_finder.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ✅ METADATA PENTRU CACHE
 * Păstrează informații despre când au fost descărcate datele.
 */
@Entity(tableName = "cache_metadata")
data class CacheMetadata(
    @PrimaryKey
    val key: String, // Ex: "stores"
    val timestamp: Long, // Când au fost descărcate
    val expiresAt: Long, // Când expiră
    val itemCount: Int = 0
)