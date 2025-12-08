package com.example.sharoma_finder.viewModel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.sharoma_finder.domain.BannerModel
import com.example.sharoma_finder.domain.CategoryModel
import com.example.sharoma_finder.domain.StoreModel
import com.example.sharoma_finder.repository.DashboardRepository
import com.example.sharoma_finder.repository.FavoritesManager
import com.example.sharoma_finder.repository.Resource
import com.example.sharoma_finder.repository.ResultsRepository

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DashboardRepository()
    private val resultsRepository = ResultsRepository()
    private val favoritesManager = FavoritesManager(application.applicationContext)

    val favoriteStoreIds = mutableStateListOf<String>()
    val favoriteStores = mutableStateListOf<StoreModel>()
    private val allStores = mutableStateListOf<StoreModel>()
    val isDataLoaded = mutableStateOf(false)

    init {
        Log.d("DashboardViewModel", "=== INIT START ===")
        loadFavorites()
        loadAllStoresData()
    }

    private fun loadFavorites() {
        favoriteStoreIds.clear()
        val savedFavorites = favoritesManager.getFavorites()
        favoriteStoreIds.addAll(savedFavorites)
        Log.d("DashboardViewModel", "✅ Loaded ${favoriteStoreIds.size} saved favorites from SharedPreferences")
        savedFavorites.forEach {
            Log.d("DashboardViewModel", "   - Saved favorite ID: $it")
        }
    }

    private fun loadAllStoresData() {
        var loadedCount = 0
        val totalLoads = 4

        fun checkIfAllLoaded() {
            loadedCount++
            Log.d("DashboardViewModel", "📦 Loaded $loadedCount/$totalLoads sources")
            if (loadedCount >= totalLoads) {
                isDataLoaded.value = true
                Log.d("DashboardViewModel", "✅ ALL DATA LOADED! Total stores in memory: ${allStores.size}")
                allStores.forEach { store ->
                    Log.d("DashboardViewModel", "   Store: ${store.Title} | Key: ${store.getUniqueId()}")
                }
                updateFavoriteStores()
            }
        }

        resultsRepository.loadPopular("1", limit = null).observeForever { resource ->
            if (resource is Resource.Success) {
                resource.data?.forEach { store ->
                    if (!allStores.any { it.getUniqueId() == store.getUniqueId() }) {
                        allStores.add(store)
                        Log.d("DashboardViewModel", "➕ Added Popular cat 1: ${store.Title} | Key: ${store.getUniqueId()}")
                    }
                }
                checkIfAllLoaded()
            } else if (resource is Resource.Error) {
                Log.e("DashboardViewModel", "❌ Error loading Popular cat 1: ${resource.message}")
                checkIfAllLoaded()
            }
        }

        resultsRepository.loadNearest("1", limit = null).observeForever { resource ->
            if (resource is Resource.Success) {
                resource.data?.forEach { store ->
                    if (!allStores.any { it.getUniqueId() == store.getUniqueId() }) {
                        allStores.add(store)
                        Log.d("DashboardViewModel", "➕ Added Nearest cat 1: ${store.Title} | Key: ${store.getUniqueId()}")
                    }
                }
                checkIfAllLoaded()
            } else if (resource is Resource.Error) {
                Log.e("DashboardViewModel", "❌ Error loading Nearest cat 1: ${resource.message}")
                checkIfAllLoaded()
            }
        }

        resultsRepository.loadPopular("2", limit = null).observeForever { resource ->
            if (resource is Resource.Success) {
                resource.data?.forEach { store ->
                    if (!allStores.any { it.getUniqueId() == store.getUniqueId() }) {
                        allStores.add(store)
                        Log.d("DashboardViewModel", "➕ Added Popular cat 2: ${store.Title} | Key: ${store.getUniqueId()}")
                    }
                }
                checkIfAllLoaded()
            } else if (resource is Resource.Error) {
                Log.e("DashboardViewModel", "❌ Error loading Popular cat 2: ${resource.message}")
                checkIfAllLoaded()
            }
        }

        resultsRepository.loadNearest("2", limit = null).observeForever { resource ->
            if (resource is Resource.Success) {
                resource.data?.forEach { store ->
                    if (!allStores.any { it.getUniqueId() == store.getUniqueId() }) {
                        allStores.add(store)
                        Log.d("DashboardViewModel", "➕ Added Nearest cat 2: ${store.Title} | Key: ${store.getUniqueId()}")
                    }
                }
                checkIfAllLoaded()
            } else if (resource is Resource.Error) {
                Log.e("DashboardViewModel", "❌ Error loading Nearest cat 2: ${resource.message}")
                checkIfAllLoaded()
            }
        }
    }

    private fun updateFavoriteStores() {
        Log.d("DashboardViewModel", "🔄 Updating favorite stores...")
        Log.d("DashboardViewModel", "   Total stores available: ${allStores.size}")
        Log.d("DashboardViewModel", "   Favorite IDs to match: ${favoriteStoreIds.size}")

        favoriteStores.clear()
        val favorites = allStores.filter { store ->
            val uniqueId = store.getUniqueId()
            val isMatch = favoriteStoreIds.contains(uniqueId)
            if (isMatch) {
                Log.d("DashboardViewModel", "   ✅ MATCH: ${store.Title} with ID: $uniqueId")
            }
            isMatch
        }

        favoriteStores.addAll(favorites)

        Log.d("DashboardViewModel", "✅ Updated favorite stores list:")
        Log.d("DashboardViewModel", "   - Total favorites in UI: ${favoriteStores.size}")
        favoriteStores.forEach {
            Log.d("DashboardViewModel", "   - ${it.Title} | ${it.getUniqueId()}")
        }

        if (favoriteStores.isEmpty() && favoriteStoreIds.isNotEmpty()) {
            Log.e("DashboardViewModel", "⚠️ WARNING: Have ${favoriteStoreIds.size} favorite IDs but 0 stores found!")
            Log.e("DashboardViewModel", "   Favorite IDs: $favoriteStoreIds")
            Log.e("DashboardViewModel", "   Available store IDs:")
            allStores.forEach { store ->
                Log.e("DashboardViewModel", "     - ${store.Title}: ${store.getUniqueId()}")
            }
        }
    }

    fun isFavorite(store: StoreModel): Boolean {
        val uniqueKey = store.getUniqueId()
        val isFav = favoriteStoreIds.contains(uniqueKey)
        Log.d("DashboardViewModel", "❓ isFavorite(${store.Title}) | Key: $uniqueKey | Result: $isFav")
        return isFav
    }

    fun toggleFavorite(store: StoreModel) {
        val uniqueKey = store.getUniqueId()
        Log.d("DashboardViewModel", "🔄 Toggle favorite: ${store.Title} | Key: $uniqueKey")

        if (favoriteStoreIds.contains(uniqueKey)) {
            favoritesManager.removeFavorite(uniqueKey)
            favoriteStoreIds.remove(uniqueKey)
            Log.d("DashboardViewModel", "❌ Removed from favorites")
        } else {
            favoritesManager.addFavorite(uniqueKey)
            favoriteStoreIds.add(uniqueKey)
            Log.d("DashboardViewModel", "➕ Added to favorites")
        }

        Log.d("DashboardViewModel", "📊 Current favorites count: ${favoriteStoreIds.size}")
        updateFavoriteStores()
    }

    fun getAllFavoriteStores(): List<StoreModel> {
        return favoriteStores
    }

    fun loadCategory(): LiveData<MutableList<CategoryModel>> {
        return repository.loadCategory()
    }

    fun loadBanner(): LiveData<MutableList<BannerModel>> {
        return repository.loadBanner()
    }
}