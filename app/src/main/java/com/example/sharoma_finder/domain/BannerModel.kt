package com.example.sharoma_finder.domain

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "banners") // ✅ Acum e salvat în Room
data class BannerModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // ID auto-generat (banner-ele nu au ID în Firebase)
    val image: String = ""
)