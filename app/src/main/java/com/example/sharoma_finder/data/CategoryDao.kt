package com.example.sharoma_finder.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sharoma_finder.domain.CategoryModel

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY Id ASC")
    fun getAllCategories(): LiveData<List<CategoryModel>>

    @Query("SELECT * FROM categories ORDER BY Id ASC")
    fun getAllCategoriesSync(): List<CategoryModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryModel>)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM categories")
    fun getCategoryCount(): Int
}