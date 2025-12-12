package com.example.sharoma_finder.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sharoma_finder.domain.StoreModel

@Dao
interface StoreDao {
    // Returnează toate magazinele. LiveData va notifica UI-ul automat la schimbări.
    @Query("SELECT * FROM stores")
    fun getAllStores(): LiveData<List<StoreModel>>

    // Returnează magazinele dintr-o categorie (pentru filtrare offline rapidă)
    @Query("SELECT * FROM stores WHERE CategoryId = :catId")
    fun getStoresByCategory(catId: String): LiveData<List<StoreModel>>

    // Inserează sau actualizează lista. Dacă există deja ID-ul, îl suprascrie.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stores: List<StoreModel>)

    // Șterge tot (folositor la refresh complet)
    @Query("DELETE FROM stores")
    suspend fun deleteAll()
}