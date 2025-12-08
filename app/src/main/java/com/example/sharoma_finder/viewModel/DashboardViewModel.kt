package com.example.sharoma_finder.viewModel

import android.app.Application
import android.location.Location
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
import com.example.sharoma_finder.repository.UserManager

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DashboardRepository()
    private val resultsRepository = ResultsRepository()
    private val favoritesManager = FavoritesManager(application.applicationContext)
    private val userManager = UserManager(application.applicationContext)

    // --- 1. LISTE PENTRU UI ---
    val favoriteStoreIds = mutableStateListOf<String>()
    val favoriteStores = mutableStateListOf<StoreModel>()

    // Lista Nearest pentru Dashboard (Top 5 cele mai apropiate)
    val nearestStoresTop5 = mutableStateListOf<StoreModel>()

    // Lista Popular pentru Dashboard (cele marcate cu IsPopular)
    val popularStores = mutableStateListOf<StoreModel>()

    // Lista Nearest COMPLETĂ și SORTATĂ (pentru See All)
    val nearestStoresAllSorted = mutableStateListOf<StoreModel>()

    // --- 2. LISTE INTERNE TEMPORARE ---
    // Lista finală unificată (MASTER)
    private val allStoresRaw = mutableListOf<StoreModel>()

    // Variabila care controlează Loading-ul
    val isDataLoaded = mutableStateOf(false)

    // --- 3. VARIABILE PENTRU PROFIL ---
    var userName = mutableStateOf("Costi")
    var userImagePath = mutableStateOf<String?>(null)

    // --- 4. LOCAȚIA UTILIZATORULUI (GPS) ---
    // Accesibilă public pentru citire (MainActivity o trimite la ResultList)
    var currentUserLocation: Location? = null
        private set

    init {
        Log.d("DashboardViewModel", "=== INIT START ===")
        loadUserData()
        loadFavorites()

        // Pornim descărcarea datelor
        loadInitialData()
    }

    // --- FUNCȚIE NOUĂ: Expunem lista completă pentru Search (ResultList) ---
    fun getGlobalStoreList(): List<StoreModel> {
        return allStoresRaw
    }

    // --- LOGICA DE ÎNCĂRCARE ȘI GPS ---

    private fun loadInitialData() {
        // Încărcăm TOATE magazinele din nodul "Stores" (baza de date unificată)
        resultsRepository.loadAllStores().observeForever { resource ->
            if (resource is Resource.Success) {
                resource.data?.let { list ->
                    allStoresRaw.clear()
                    allStoresRaw.addAll(list)

                    Log.d("DashboardVM", "📦 Loaded ${allStoresRaw.size} total stores")

                    // Procesăm datele (sortare inițială fără GPS)
                    processData()

                    // Dacă avem GPS cached, recalculăm distanțele
                    if (currentUserLocation != null) {
                        recalculateDistances()
                    }

                    isDataLoaded.value = true
                }
            }
        }
    }

    // Apelată din MainActivity când GPS-ul ne dă locația
    fun updateUserLocation(location: Location) {
        currentUserLocation = location
        Log.d("DashboardVM", "📍 User location updated: ${location.latitude}, ${location.longitude}")
        recalculateDistances()
    }

    private fun recalculateDistances() {
        val location = currentUserLocation ?: return
        if (allStoresRaw.isEmpty()) return

        Log.d("DashboardVM", "📏 Recalculating distances...")

        // 1. Calculăm distanța pentru fiecare magazin
        allStoresRaw.forEach { store ->
            val storeLoc = Location("store")
            storeLoc.latitude = store.Latitude
            storeLoc.longitude = store.Longitude
            store.distanceToUser = location.distanceTo(storeLoc)
        }

        // 2. Re-procesăm listele cu noile distanțe
        processData()
    }

    private fun processData() {
        // A. Sortăm toată lista după distanță (crescător)
        val sortedList = allStoresRaw.sortedBy {
            if (it.distanceToUser < 0) Float.MAX_VALUE else it.distanceToUser
        }

        // B. Populăm Nearest Top 5
        nearestStoresTop5.clear()
        nearestStoresTop5.addAll(sortedList.take(5))

        // C. Populăm lista completă sortată (pentru See All)
        nearestStoresAllSorted.clear()
        nearestStoresAllSorted.addAll(sortedList)

        // D. Populăm Popular Stores (filtrăm după IsPopular din lista deja sortată/calculată)
        // --- AICI ERA PROBLEMA ANTERIOARĂ: Acum luăm din 'sortedList' care are distanțele calculate ---
        val popular = sortedList.filter { it.IsPopular }
        popularStores.clear()
        popularStores.addAll(popular)

        // E. Actualizăm favoritele (ca să aibă și ele distanța actualizată)
        updateFavoriteStores()

        Log.d("DashboardVM", "✅ Data processed. Nearest: ${nearestStoresTop5.size}, Popular: ${popularStores.size}")
    }

    // --- LOGICA PENTRU PROFIL ---

    private fun loadUserData() {
        userName.value = userManager.getName()
        userImagePath.value = userManager.getImagePath()
    }

    fun updateUserName(newName: String) {
        userName.value = newName
        userManager.saveName(newName)
    }

    fun updateUserImage(uri: android.net.Uri) {
        val internalPath = userManager.copyImageToInternalStorage(uri)
        if (internalPath != null) {
            userImagePath.value = internalPath
            userManager.saveImagePath(internalPath)
        }
    }

    // --- LOGICA PENTRU FAVORITE ---

    private fun loadFavorites() {
        favoriteStoreIds.clear()
        favoriteStoreIds.addAll(favoritesManager.getFavorites())
    }

    private fun updateFavoriteStores() {
        // Filtrăm din lista completă (allStoresRaw) doar pe cele favorite
        val favorites = allStoresRaw.filter { store ->
            favoriteStoreIds.contains(store.getUniqueId())
        }

        // Sortăm și favoritele după distanță
        val sortedFavorites = favorites.sortedBy {
            if (it.distanceToUser < 0) Float.MAX_VALUE else it.distanceToUser
        }

        favoriteStores.clear()
        favoriteStores.addAll(sortedFavorites)
        Log.d("DashboardViewModel", "🔄 Wishlist updated & sorted: ${favoriteStores.size} stores shown")
    }

    fun isFavorite(store: StoreModel): Boolean = favoriteStoreIds.contains(store.getUniqueId())

    fun toggleFavorite(store: StoreModel) {
        val uniqueKey = store.getUniqueId()
        if (favoriteStoreIds.contains(uniqueKey)) {
            favoritesManager.removeFavorite(uniqueKey)
            favoriteStoreIds.remove(uniqueKey)
        } else {
            favoritesManager.addFavorite(uniqueKey)
            favoriteStoreIds.add(uniqueKey)
        }
        updateFavoriteStores()
    }

    // --- ALTE FUNCȚII ---
    fun loadCategory(): LiveData<MutableList<CategoryModel>> = repository.loadCategory()
    fun loadBanner(): LiveData<MutableList<BannerModel>> = repository.loadBanner()
}