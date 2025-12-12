package com.example.sharoma_finder.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sharoma_finder.domain.StoreModel

@Dao
interface StoreDao {
    // LiveData pentru observare automată (folosit în ViewModel)
    @Query("SELECT * FROM stores")
    fun getAllStores(): LiveData<List<StoreModel>>

    // ✅ ADĂUGAT: Versiune sincronă pentru Repository-uri (funcționează instant, fără LiveData)
    @Query("SELECT * FROM stores")
    fun getAllStoresSync(): List<StoreModel>

    // Filtrare după categorie (ambele versiuni)
    @Query("SELECT * FROM stores WHERE CategoryId = :catId")
    fun getStoresByCategory(catId: String): LiveData<List<StoreModel>>

    @Query("SELECT * FROM stores WHERE CategoryId = :catId")
    fun getStoresByCategorySync(catId: String): List<StoreModel>

    // Inserare/update
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stores: List<StoreModel>)

    // Ștergere completă
    @Query("DELETE FROM stores")
    suspend fun deleteAll()

    // ✅ BONUS: Verifică dacă există date în cache
    @Query("SELECT COUNT(*) FROM stores")
    fun getStoreCount(): Int
}