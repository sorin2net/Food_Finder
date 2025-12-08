package com.example.sharoma_finder.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.sharoma_finder.domain.CategoryModel
import com.example.sharoma_finder.domain.StoreModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

class ResultsRepository {
    private val firebaseDatabase = FirebaseDatabase.getInstance()

    // --- FUNCȚIA PRINCIPALĂ: Încarcă TOATE magazinele din nodul 'Stores' ---
    // Aceasta este folosită de DashboardViewModel pentru a calcula distanțele GPS
    fun loadAllStores(): LiveData<Resource<MutableList<StoreModel>>> {
        val listData = MutableLiveData<Resource<MutableList<StoreModel>>>()
        listData.value = Resource.Loading()

        val ref = firebaseDatabase.getReference("Stores")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lists = mutableListOf<StoreModel>()
                for (child in snapshot.children) {
                    val model = child.getValue(StoreModel::class.java)
                    if (model != null) {
                        // Salvăm cheia originală din Firebase (ex: "store_01")
                        model.firebaseKey = child.key ?: ""
                        lists.add(model)
                    }
                }
                // Log pentru verificare
                Log.d("ResultsRepository", "Loaded ${lists.size} total stores")
                listData.value = Resource.Success(lists)
            }
            override fun onCancelled(error: DatabaseError) {
                listData.value = Resource.Error(error.message)
            }
        })
        return listData
    }

    // --- Funcții pentru filtrarea pe categorii (folosite în ecranele de detalii) ---

    fun loadSubCategory(id: String): LiveData<Resource<MutableList<CategoryModel>>> {
        val listData = MutableLiveData<Resource<MutableList<CategoryModel>>>()
        listData.value = Resource.Loading()
        val ref = firebaseDatabase.getReference("SubCategory")
        val query: Query = ref.orderByChild("CategoryId").equalTo(id)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lists = mutableListOf<CategoryModel>()
                for (child in snapshot.children) {
                    val model = child.getValue(CategoryModel::class.java)
                    if (model != null) lists.add(model)
                }
                listData.value = Resource.Success(lists)
            }
            override fun onCancelled(error: DatabaseError) { listData.value = Resource.Error(error.message) }
        })
        return listData
    }

    // Încarcă magazinele pentru o anumită categorie (ex: doar Burgeri)
    fun loadPopular(id: String, limit: Int? = null): LiveData<Resource<MutableList<StoreModel>>> {
        val listData = MutableLiveData<Resource<MutableList<StoreModel>>>()
        listData.value = Resource.Loading()

        val ref = firebaseDatabase.getReference("Stores")
        var query: Query = ref.orderByChild("CategoryId").equalTo(id)

        if (limit != null) {
            query = query.limitToFirst(limit)
        }

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lists = mutableListOf<StoreModel>()
                for (child in snapshot.children) {
                    val model = child.getValue(StoreModel::class.java)
                    if (model != null) {
                        model.firebaseKey = child.key ?: ""
                        lists.add(model)
                    }
                }
                listData.value = Resource.Success(lists)
            }
            override fun onCancelled(error: DatabaseError) { listData.value = Resource.Error(error.message) }
        })
        return listData
    }

    // Pentru compatibilitate, loadNearest acum trage tot din "Stores"
    // (deoarece nodul "Nearest" nu mai există în noul JSON)
    fun loadNearest(id: String, limit: Int? = null): LiveData<Resource<MutableList<StoreModel>>> {
        return loadPopular(id, limit)
    }
}