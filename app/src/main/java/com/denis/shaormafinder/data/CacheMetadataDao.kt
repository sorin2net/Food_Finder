package com.denis.shaormafinder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.denis.shaormafinder.domain.CacheMetadata

@Dao
interface CacheMetadataDao {
    @Query("SELECT * FROM cache_metadata WHERE `key` = :key")
    suspend fun getMetadata(key: String): CacheMetadata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMetadata(metadata: CacheMetadata)

    @Query("DELETE FROM cache_metadata WHERE `key` = :key")
    suspend fun deleteMetadata(key: String)

    @Query("DELETE FROM cache_metadata")
    suspend fun clearAll()
}