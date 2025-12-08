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

    // Lista Nearest COMPLETĂ și SORTATĂ (pentru See All)
    val nearestStoresAllSorted = mutableStateListOf<StoreModel>()

    // --- 2. LISTE INTERNE TEMPORARE (pentru procesare) ---
    // Lista finală unificată pentru calcule
    private val allStoresRaw = mutableListOf<StoreModel>()
    // Liste temporare pentru a stoca datele pe măsură ce vin din Firebase
    private val tempStoreList = mutableListOf<StoreModel>()
    private val tempNearestList = mutableListOf<StoreModel>()

    // Variabila care controlează Loading-ul din Wishlist și Nearest
    val isDataLoaded = mutableStateOf(false)

    // --- 3. VARIABILE PENTRU PROFIL ---
    var userName = mutableStateOf("Costi")
    var userImagePath = mutableStateOf<String?>(null)

    // --- 4. LOCAȚIA UTILIZATORULUI (GPS) ---
    // AM SCOS "private" DE AICI. Acum e accesibilă din MainActivity.
    var currentUserLocation: Location? = null
        private set // Putem lăsa asta ca să fie modificată doar din interiorul clasei, dar citită de oriunde

    init {
        Log.d("DashboardViewModel", "=== INIT START ===")
        loadUserData()
        loadFavorites()

        // Pornim descărcarea datelor (Load All pentru GPS)
        loadInitialData()
    }

    // --- FUNCȚIE NOUĂ: Expunem lista completă pentru Search (ResultList) ---
    fun getGlobalStoreList(): List<StoreModel> {
        return allStoresRaw
    }

    // --- LOGICA DE ÎNCĂRCARE ȘI GPS ---

    private fun loadInitialData() {
        // 1. Încărcăm nodul "Stores"
        resultsRepository.loadAllStoresForGPS().observeForever { resource ->
            if (resource is Resource.Success) {
                resource.data?.let { list ->
                    tempStoreList.clear()
                    tempStoreList.addAll(list)
                    combineAndRefresh() // Încercăm să combinăm datele
                }
            }
        }

        // 2. Încărcăm nodul "Nearest"
        resultsRepository.loadAllNearestForGPS().observeForever { resource ->
            if (resource is Resource.Success) {
                resource.data?.let { list ->
                    tempNearestList.clear()
                    tempNearestList.addAll(list)
                    combineAndRefresh() // Încercăm să combinăm datele
                }
            }
        }
    }

    // Funcție care unește cele două surse de date (Stores + Nearest)
    private fun combineAndRefresh() {
        // 1. Punem toate magazinele din "Stores"
        allStoresRaw.clear()
        allStoresRaw.addAll(tempStoreList)

        // 2. Adăugăm magazinele din "Nearest", dar verificăm să nu fie duplicate
        tempNearestList.forEach { nearestItem ->
            // Folosim getUniqueId() care include categoryId și Id sau firebaseKey
            if (allStoresRaw.none { it.getUniqueId() == nearestItem.getUniqueId() }) {
                allStoresRaw.add(nearestItem)
            }
        }

        Log.d("DashboardVM", "📦 Total stores combined: ${allStoresRaw.size}")

        // 3. Recalculăm distanțele dacă avem GPS, altfel afișăm datele brute
        if (currentUserLocation != null) {
            recalculateDistances()
        } else {
            // Fallback dacă nu avem GPS: arătăm primele 5 așa cum sunt
            nearestStoresTop5.clear()
            nearestStoresTop5.addAll(allStoresRaw.take(5))
        }

        isDataLoaded.value = true
        updateFavoriteStores()
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

        Log.d("DashboardVM", "📏 Calculating distances for ${allStoresRaw.size} stores...")

        // 1. Calculăm distanța pentru fiecare magazin
        allStoresRaw.forEach { store ->
            val storeLoc = Location("store")
            storeLoc.latitude = store.Latitude
            storeLoc.longitude = store.Longitude

            // Distanța în metri
            store.distanceToUser = location.distanceTo(storeLoc)
        }

        // 2. Sortăm crescător după distanță (cel mai mic -> cel mai mare)
        val sortedList = allStoresRaw.sortedBy { it.distanceToUser }

        // 3. Actualizăm listele pentru UI
        nearestStoresAllSorted.clear()
        nearestStoresAllSorted.addAll(sortedList)

        nearestStoresTop5.clear()
        nearestStoresTop5.addAll(sortedList.take(5))

        Log.d("DashboardVM", "✅ Nearest list updated. Closest: ${sortedList.firstOrNull()?.Title}")

        // Re-actualizăm favoritele pentru că obiectele din allStoresRaw s-au schimbat (au primit distanță)
        updateFavoriteStores()
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
        // Dacă distanța e -1 (necalculată), le punem la final
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