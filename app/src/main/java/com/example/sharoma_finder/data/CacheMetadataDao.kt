package com.example.sharoma_finder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sharoma_finder.domain.CacheMetadata

@Dao
interface CacheMetadataDao {
    // ✅ FIX: Am pus `key` între backticks (`...`) pentru a-l scăpa de cuvântul rezervat SQL
    @Query("SELECT * FROM cache_metadata WHERE `key` = :key")
    suspend fun getMetadata(key: String): CacheMetadata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMetadata(metadata: CacheMetadata)

    // ✅ FIX: Și aici am adăugat backticks
    @Query("DELETE FROM cache_metadata WHERE `key` = :key")
    suspend fun deleteMetadata(key: String)

    @Query("DELETE FROM cache_metadata")
    suspend fun clearAll()
}