package com.example.sharoma_finder.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.sharoma_finder.data.StoreDao
import com.example.sharoma_finder.domain.StoreModel
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class StoreRepository(private val storeDao: StoreDao) {
    private val firebaseDatabase = FirebaseDatabase.getInstance()

    // Sursa de adevăr este baza de date locală
    val allStores: LiveData<List<StoreModel>> = storeDao.getAllStores()

    /**
     * Sincronizează datele cu Firebase
     * ✅ Funcționează OFFLINE - nu blochează aplicația dacă nu e internet
     * ✅ Are TIMEOUT - nu așteaptă la infinit
     * ✅ Gestionează erorile ELEGANT - nu face crash aplicația
     */
    suspend fun refreshStores() {
        withContext(Dispatchers.IO) {
            try {
                Log.d("StoreRepository", "🌍 Starting Firebase sync...")

                // ✅ ADĂUGAT: Timeout de 10 secunde pentru Firebase
                // Dacă nu răspunde în 10 secunde, renunțăm și folosim cache-ul local
                val snapshot = withTimeoutOrNull(10000L) {
                    firebaseDatabase.getReference("Stores").get().await()
                }

                if (snapshot == null) {
                    Log.w("StoreRepository", "⏰ Firebase timeout - using local cache")
                    return@withContext
                }

                val freshStores = mutableListOf<StoreModel>()
                var invalidCount = 0

                for (child in snapshot.children) {
                    val model = child.getValue(StoreModel::class.java)
                    if (model != null && model.isValid()) {
                        model.firebaseKey = child.key ?: "${model.CategoryId}_${model.Id}"
                        freshStores.add(model)
                    } else {
                        invalidCount++
                    }
                }

                if (freshStores.isNotEmpty()) {
                    Log.d("StoreRepository", "✅ Synced ${freshStores.size} stores ($invalidCount invalid)")

                    // ✅ Salvăm în Room (actualizează automat LiveData)
                    storeDao.insertAll(freshStores)

                    Log.d("StoreRepository", "💾 Successfully saved to local database")
                } else {
                    Log.w("StoreRepository", "⚠️ Firebase returned empty list")
                }

            } catch (e: com.google.firebase.FirebaseException) {
                Log.e("StoreRepository", "🔥 Firebase error: ${e.message}")
                // Firebase error (de obicei offline) - nu facem nimic, folosim cache-ul
            } catch (e: java.net.UnknownHostException) {
                Log.e("StoreRepository", "🌐 No internet connection")
                // Offline - normal, folosim cache-ul local
            } catch (e: java.net.SocketTimeoutException) {
                Log.e("StoreRepository", "⏰ Connection timeout")
                // Timeout - folosim cache-ul local
            } catch (e: Exception) {
                Log.e("StoreRepository", "❌ Unexpected error: ${e.javaClass.simpleName} - ${e.message}")
                // Orice altă eroare - nu blocăm aplicația
            }
        }
    }

    /**
     * ✅ NOU: Funcție pentru a verifica dacă avem date în cache
     * Folositor pentru debugging și UI
     */
    suspend fun hasCachedData(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                storeDao.getStoreCount() > 0
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * ✅ NOU: Șterge toate datele locale (pentru debugging sau logout)
     */
    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            try {
                storeDao.deleteAll()
                Log.d("StoreRepository", "🗑️ Cache cleared successfully")
            } catch (e: Exception) {
                Log.e("StoreRepository", "Error clearing cache: ${e.message}")
            }
        }
    }
}