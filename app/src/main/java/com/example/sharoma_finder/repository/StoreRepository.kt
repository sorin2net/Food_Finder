package com.example.sharoma_finder.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.sharoma_finder.data.StoreDao
import com.example.sharoma_finder.domain.StoreModel
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class StoreRepository(private val storeDao: StoreDao) {
    private val firebaseDatabase = FirebaseDatabase.getInstance()

    // ✅ Sursa de adevăr este ACUM Baza de Date Locală (Room)
    // UI-ul va observa această listă. Când Room se updatează, UI-ul se updatează.
    val allStores: LiveData<List<StoreModel>> = storeDao.getAllStores()

    // ✅ Funcție apelată din ViewModel pentru a sincroniza datele
    suspend fun refreshStores() {
        withContext(Dispatchers.IO) {
            try {
                Log.d("StoreRepository", "🌍 Fetching stores from Firebase...")

                // 1. Luăm datele din Firebase (folosind await() pentru coroutines)
                val snapshot = firebaseDatabase.getReference("Stores").get().await()
                val freshStores = mutableListOf<StoreModel>()

                for (child in snapshot.children) {
                    val model = child.getValue(StoreModel::class.java)
                    if (model != null && model.isValid()) {
                        // Asigurăm cheia unică
                        model.firebaseKey = child.key ?: "${model.CategoryId}_${model.Id}"
                        freshStores.add(model)
                    }
                }

                // 2. Salvăm în Room (Room va notifica automat LiveData-ul de mai sus)
                if (freshStores.isNotEmpty()) {
                    Log.d("StoreRepository", "💾 Saving ${freshStores.size} stores to Room")
                    storeDao.insertAll(freshStores)
                } else {
                    Log.w("StoreRepository", "⚠️ Firebase returned empty list")
                }

            } catch (e: Exception) {
                Log.e("StoreRepository", "❌ Error syncing data: ${e.message}")
                // Nu facem nimic critic aici. Dacă pică netul,
                // utilizatorul rămâne cu datele vechi din Room (allStores).
            }
        }
    }
}