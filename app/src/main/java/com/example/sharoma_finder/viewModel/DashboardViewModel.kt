package com.example.sharoma_finder.viewModel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
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

    private val allPopularStores = mutableStateListOf<StoreModel>()
    private val allNearestStores = mutableStateListOf<StoreModel>()

    init {
        loadFavorites()
        loadAllStoresData()
    }

    private fun loadFavorites() {
        favoriteStoreIds.clear()
        favoriteStoreIds.addAll(favoritesManager.getFavorites())
    }

    private fun loadAllStoresData() {
        resultsRepository.loadPopular("").observeForever { resource ->
            if (resource is Resource.Success) {
                allPopularStores.clear()
                resource.data?.let { allPopularStores.addAll(it) }
            }
        }
        resultsRepository.loadNearest("").observeForever { resource ->
            if (resource is Resource.Success) {
                allNearestStores.clear()
                resource.data?.let { allNearestStores.addAll(it) }
            }
        }
    }

    fun isFavorite(storeId: Int): Boolean {
        return favoriteStoreIds.contains(storeId.toString())
    }

    fun toggleFavorite(storeId: Int) {
        val idString = storeId.toString()
        if (favoriteStoreIds.contains(idString)) {

            favoritesManager.removeFavorite(storeId)
            favoriteStoreIds.remove(idString)
        } else {

            favoritesManager.addFavorite(storeId)
            favoriteStoreIds.add(idString)
        }

    }

    fun getAllFavoriteStores(): List<StoreModel> {
        val allStores = (allPopularStores + allNearestStores).distinctBy { it.Id }
        return allStores.filter { favoriteStoreIds.contains(it.Id.toString()) }
    }

    fun loadCategory(): LiveData<MutableList<CategoryModel>> {
        return repository.loadCategory()
    }

    fun loadBanner(): LiveData<MutableList<BannerModel>> {
        return repository.loadBanner()
    }
}