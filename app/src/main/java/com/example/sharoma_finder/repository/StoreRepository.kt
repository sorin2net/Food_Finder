package com.example.sharoma_finder.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.sharoma_finder.data.CacheMetadataDao // ✅ Import
import com.example.sharoma_finder.data.StoreDao
import com.example.sharoma_finder.domain.CacheMetadata // ✅ Import
import com.example.sharoma_finder.domain.StoreModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class StoreRepository(
    private val storeDao: StoreDao,
    private val cacheMetadataDao: CacheMetadataDao // ✅ ADĂUGAT în constructor
) {
    private val firebaseDatabase = FirebaseDatabase.getInstance()

    val allStores: LiveData<List<StoreModel>> = storeDao.getAllStores()

    companion object {
        private const val CACHE_KEY_STORES = "stores"
        private const val CACHE_VALIDITY_HOURS = 6L // Cache valabil 6 ore
    }

    // ✅ FUNCȚIE NOUĂ: Verifică dacă datele sunt proaspete
    private suspend fun isCacheValid(): Boolean {
        return try {
            val metadata = cacheMetadataDao.getMetadata(CACHE_KEY_STORES)
            if (metadata == null) return false

            val now = System.currentTimeMillis()
            val isValid = now < metadata.expiresAt

            if (isValid) {
                // Afișăm cât timp mai e valid cache-ul (în minute)
                val remainingMinutes = (metadata.expiresAt - now) / 60000
                Log.d("StoreRepository", "✅ Cache valid for $remainingMinutes more minutes")
            } else {
                Log.d("StoreRepository", "⏰ Cache EXPIRED (or missing)")
            }
            isValid
        } catch (e: Exception) {
            Log.e("StoreRepository", "Error checking cache: ${e.message}")
            false
        }
    }

    /**
     * ✅ VERSIUNE FINALĂ: Parsing manual + Cache Expiration
     */
    suspend fun refreshStores(forceRefresh: Boolean = false) { // ✅ Parametru nou opțional
        withContext(Dispatchers.IO) {
            try {
                // ✅ VERIFICARE CACHE: Dacă nu forțăm și cache-ul e valid, ne oprim aici
                // Asta economisește date și baterie!
                if (!forceRefresh && isCacheValid()) {
                    Log.d("StoreRepository", "📦 Using cached data (still fresh)")
                    return@withContext
                }

                Log.d("StoreRepository", "🌍 Starting Firebase sync...")

                val snapshot = withTimeoutOrNull(15000L) {
                    firebaseDatabase.getReference("Stores").get().await()
                }

                if (snapshot == null) {
                    Log.w("StoreRepository", "⏰ Firebase timeout - using cache")
                    return@withContext
                }

                Log.d("StoreRepository", "📦 Snapshot exists: ${snapshot.exists()}, children: ${snapshot.childrenCount}")

                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    Log.w("StoreRepository", "⚠️ Firebase returned empty - keeping cache")
                    return@withContext
                }

                val freshStores = mutableListOf<StoreModel>()
                var invalidCount = 0
                var parseErrorCount = 0

                for (child in snapshot.children) {
                    try {
                        // ✅ PARSING MANUAL pentru a gestiona CategoryId numeric/String
                        val model = parseStoreFromSnapshot(child)

                        if (model == null) {
                            parseErrorCount++
                            Log.w("StoreRepository", "⚠️ Failed to parse: ${child.key}")
                            continue
                        }

                        if (model.isValid()) {
                            model.firebaseKey = child.key ?: "${model.CategoryId}_${model.Id}"
                            freshStores.add(model)
                        } else {
                            invalidCount++
                            Log.w("StoreRepository", "⚠️ Invalid: ${model.Title}")
                        }
                    } catch (e: Exception) {
                        parseErrorCount++
                        Log.e("StoreRepository", "❌ Parse error for ${child.key}: ${e.message}")
                    }
                }

                Log.d("StoreRepository", "📊 Results: ✅ ${freshStores.size} valid, ⚠️ $invalidCount invalid, ❌ $parseErrorCount errors")

                if (freshStores.isEmpty()) {
                    Log.e("StoreRepository", "❌ ZERO valid stores - keeping cache")
                    return@withContext
                }

                // ✅ Salvăm datele valide
                storeDao.insertAll(freshStores)

                // ✅ SALVARE METADATA: Marcăm momentul descărcării
                val now = System.currentTimeMillis()
                val expiresAt = now + (CACHE_VALIDITY_HOURS * 60 * 60 * 1000)

                cacheMetadataDao.saveMetadata(
                    CacheMetadata(
                        key = CACHE_KEY_STORES,
                        timestamp = now,
                        expiresAt = expiresAt,
                        itemCount = freshStores.size
                    )
                )

                Log.d("StoreRepository", "💾 Saved ${freshStores.size} stores to cache (valid for $CACHE_VALIDITY_HOURS hours)")

            } catch (e: Exception) {
                Log.e("StoreRepository", "❌ Error: ${e.javaClass.simpleName} - ${e.message}")
            }
        }
    }

    /**
     * ✅ FUNCȚIE HELPER: Parsează manual un store din Firebase
     * Gestionează CategoryId atât ca Int cât și ca String
     */
    private fun parseStoreFromSnapshot(snapshot: DataSnapshot): StoreModel? {
        try {
            val map = snapshot.value as? Map<*, *> ?: return null

            // ✅ Convertește CategoryId (poate fi Int sau String)
            val categoryId = when (val catId = map["CategoryId"]) {
                is Long -> catId.toString()
                is Int -> catId.toString()
                is String -> catId
                else -> ""
            }

            // ✅ Parsează Tags (poate fi List sau null)
            val tags = when (val tagData = map["Tags"]) {
                is List<*> -> tagData.mapNotNull { it as? String }
                else -> emptyList()
            }

            return StoreModel(
                Id = (map["Id"] as? Long)?.toInt() ?: 0,
                CategoryId = categoryId,
                Title = map["Title"] as? String ?: "",
                Address = map["Address"] as? String ?: "",
                ShortAddress = map["ShortAddress"] as? String ?: "",
                Activity = map["Activity"] as? String ?: "",
                Call = map["Call"] as? String ?: "",
                Hours = map["Hours"] as? String ?: "",
                Latitude = (map["Latitude"] as? Double) ?: 0.0,
                Longitude = (map["Longitude"] as? Double) ?: 0.0,
                ImagePath = map["ImagePath"] as? String ?: "",
                IsPopular = map["IsPopular"] as? Boolean ?: false,
                Tags = tags
            )
        } catch (e: Exception) {
            Log.e("StoreRepository", "Parse exception: ${e.message}")
            return null
        }
    }

    suspend fun hasCachedData(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                storeDao.getStoreCount() > 0
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            try {
                storeDao.deleteAll()
                // ✅ Ștergem și metadata când curățăm cache-ul
                cacheMetadataDao.deleteMetadata(CACHE_KEY_STORES)
                Log.d("StoreRepository", "🗑️ Cache cleared")
            } catch (e: Exception) {
                Log.e("StoreRepository", "Error clearing cache: ${e.message}")
            }
        }
    }
}