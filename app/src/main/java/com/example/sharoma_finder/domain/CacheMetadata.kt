package com.example.sharoma_finder.domain

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "cache_metadata")
data class CacheMetadata(
    @PrimaryKey
    val key: String,
    val timestamp: Long,
    val expiresAt: Long,
    val itemCount: Int = 0
)