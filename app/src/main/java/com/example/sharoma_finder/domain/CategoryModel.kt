package com.example.sharoma_finder.domain

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "categories") // ✅ Acum e salvat în Room
data class CategoryModel(
    @PrimaryKey
    var Id: Int = 0,
    var ImagePath: String = "",
    var Name: String = ""
)