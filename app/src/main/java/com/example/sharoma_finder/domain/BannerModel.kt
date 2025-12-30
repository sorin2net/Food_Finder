package com.example.sharoma_finder.domain

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "banners")
data class BannerModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val image: String = ""
)