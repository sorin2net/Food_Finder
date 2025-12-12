package com.example.sharoma_finder.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sharoma_finder.domain.BannerModel

@Dao
interface BannerDao {
    @Query("SELECT * FROM banners")
    fun getAllBanners(): LiveData<List<BannerModel>>

    @Query("SELECT * FROM banners")
    fun getAllBannersSync(): List<BannerModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(banners: List<BannerModel>)

    @Query("DELETE FROM banners")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM banners")
    fun getBannerCount(): Int
}