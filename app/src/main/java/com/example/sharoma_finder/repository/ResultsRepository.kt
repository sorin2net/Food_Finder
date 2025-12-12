package com.example.sharoma_finder.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.sharoma_finder.data.StoreDao
import com.example.sharoma_finder.domain.CategoryModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

/**
 * ✅ VERSIUNE OFFLINE-FIRST
 *
 * Repository-ul ăsta încarcă DOAR subcategoriile (Burger, Pizza, etc.)
 * Magazinele se încarcă prin StoreRepository → DashboardViewModel
 */
class ResultsRepository(private val storeDao: StoreDao) {
    private val firebaseDatabase = FirebaseDatabase.getInstance()

    /**
     * ✅ Încarcă subcategoriile (Burger, Pizza, Sushi)
     * Acestea sunt necesare doar pentru filtrarea UI-ului, nu pentru date critice
     */
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

                Log.d("ResultsRepository", "Loaded ${lists.size} subcategories for category $id")
                listData.value = Resource.Success(lists)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ResultsRepository", "Error loading subcategories: ${error.message}")
                // ✅ MODIFICAT: Trimitem listă goală în loc de eroare
                // Subcategoriile sunt opționale - aplicația funcționează și fără ele
                listData.value = Resource.Success(mutableListOf())
            }
        })

        return listData
    }

    // ✅ ELIMINAT: loadPopular() și loadNearest()
    // Acestea se procesează acum în DashboardViewModel din cache-ul local
}