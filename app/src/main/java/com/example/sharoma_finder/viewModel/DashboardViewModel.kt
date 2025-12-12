package com.example.sharoma_finder.viewModel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.example.sharoma_finder.data.AppDatabase
import com.example.sharoma_finder.domain.BannerModel
import com.example.sharoma_finder.domain.CategoryModel
import com.example.sharoma_finder.domain.StoreModel
import com.example.sharoma_finder.repository.DashboardRepository
import com.example.sharoma_finder.repository.FavoritesManager
import com.example.sharoma_finder.repository.StoreRepository
import com.example.sharoma_finder.repository.UserManager
import com.google.android.gms.location.LocationServices
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    // Repository-uri vechi (pentru Bannere și Categorii - rămân pe Firebase direct momentan)
    private val dashboardRepository = DashboardRepository()

    // Manageri locali
    private val favoritesManager = FavoritesManager(application.applicationContext)
    private val userManager = UserManager(application.applicationContext)
    private val analytics = FirebaseAnalytics.getInstance(application.applicationContext)

    // ✅ OFFLINE SUPPORT: Inițializare Room Database și StoreRepository
    private val database = AppDatabase.getDatabase(application)
    private val storeRepository = StoreRepository(database.storeDao())

    // Observer pentru baza de date locală (îl păstrăm ca variabilă ca să îl putem opri la onCleared)
    private lateinit var localStoreObserver: Observer<List<StoreModel>>

    // Liste pentru UI (State)
    val favoriteStoreIds = mutableStateListOf<String>()
    val favoriteStores = mutableStateListOf<StoreModel>()
    val nearestStoresTop5 = mutableStateListOf<StoreModel>()
    val popularStores = mutableStateListOf<StoreModel>()
    val nearestStoresAllSorted = mutableStateListOf<StoreModel>()

    // Lista internă cu toate magazinele (sursa brută)
    private val allStoresRaw = mutableListOf<StoreModel>()

    // State UI
    val isDataLoaded = mutableStateOf(false)
    var userName = mutableStateOf("Utilizatorule")
    var userImagePath = mutableStateOf<String?>(null)
    var currentUserLocation: Location? = null
        private set

    init {
        Log.d("DashboardViewModel", "=== INIT START ===")
        loadUserData()
        loadFavorites()

        // ✅ 1. Pornim observarea Bazei de Date Locale (Room)
        observeLocalDatabase()

        // ✅ 2. Lansăm sincronizarea cu Firebase în fundal
        refreshDataFromNetwork()
    }

    private fun observeLocalDatabase() {
        // Definim observer-ul
        localStoreObserver = Observer { stores ->
            // ✅ MODIFICARE: Procesăm lista chiar dacă e goală (nu punem if isNotEmpty la început)
            if (stores != null) {
                Log.d("DashboardVM", "🏠 Loaded ${stores.size} stores from LOCAL DB (Room)")

                // 1. Punem datele din DB în lista internă (chiar dacă e goală, o curățăm pe cea veche)
                allStoresRaw.clear()
                allStoresRaw.addAll(stores)

                // 2. Procesăm datele (calculăm distanțe dacă avem GPS, sortăm, filtrăm)
                if (currentUserLocation != null) {
                    recalculateDistances()
                } else {
                    processData() // Doar sortează/filtrează fără distanță
                }

                // ✅ Dacă avem date locale, oprim loading-ul imediat.
                // Dacă lista e goală, așteptăm refreshDataFromNetwork să oprească loading-ul.
                if (stores.isNotEmpty()) {
                    isDataLoaded.value = true
                }
            }
        }

        // Atașăm observer-ul la LiveData-ul din Repository
        storeRepository.allStores.observeForever(localStoreObserver)
    }

    private fun refreshDataFromNetwork() {
        viewModelScope.launch {
            Log.d("DashboardVM", "🔄 Starting network sync...")

            // Apelăm funcția din repository. Aceasta rulează și dacă nu e net (prinde eroarea intern).
            storeRepository.refreshStores()

            // ✅ FIX CRITIC: Indiferent dacă a reușit sau a eșuat (ex: offline),
            // marcăm datele ca fiind încărcate. Astfel dispare loading-ul infinit.
            isDataLoaded.value = true

            Log.d("DashboardVM", "✅ Network sync finished (or skipped). Loading stopped.")
        }
    }

    // --- LOGICA DE LOCAȚIE ---
    fun fetchUserLocation() {
        val context = getApplication<Application>().applicationContext

        val hasFine = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            updateUserLocation(location)
                            Log.d("DashboardVM", "GPS location found: ${location.latitude}, ${location.longitude}")
                        } else {
                            Log.w("DashboardVM", "GPS enabled but location is null")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("DashboardVM", "Failed to get GPS location", exception)
                    }
            } catch (e: SecurityException) {
                Log.e("DashboardVM", "GPS Security Error", e)
            }
        } else {
            Log.w("DashboardVM", "Cannot fetch location: Permissions missing")
        }
    }

    fun updateUserLocation(location: Location) {
        currentUserLocation = location
        // Când primim locația, recalculăm distanțele pentru magazinele deja încărcate
        recalculateDistances()
    }

    private fun recalculateDistances() {
        val location = currentUserLocation ?: return
        if (allStoresRaw.isEmpty()) return

        Log.d("DashboardVM", "📏 Recalculating distances...")

        // Calculăm distanța pentru fiecare magazin
        allStoresRaw.forEach { store ->
            val storeLoc = Location("store")
            storeLoc.latitude = store.Latitude
            storeLoc.longitude = store.Longitude
            store.distanceToUser = location.distanceTo(storeLoc)
        }

        // Reprocesăm listele (sortare după distanță nouă)
        processData()
    }

    private fun processData() {
        // Sortăm lista principală: cele mai apropiate primele
        val sortedList = allStoresRaw.sortedBy {
            if (it.distanceToUser < 0) Float.MAX_VALUE else it.distanceToUser
        }

        // Umplem listele pentru UI
        nearestStoresTop5.clear()
        nearestStoresTop5.addAll(sortedList.take(5))

        nearestStoresAllSorted.clear()
        nearestStoresAllSorted.addAll(sortedList)

        val popular = sortedList.filter { it.IsPopular }
        popularStores.clear()
        popularStores.addAll(popular)

        updateFavoriteStores()

        Log.d("DashboardVM", "✅ Data processed. Stores: ${allStoresRaw.size}")
    }

    override fun onCleared() {
        super.onCleared()
        // Curățăm observer-ul pentru a evita memory leaks
        if (::localStoreObserver.isInitialized) {
            storeRepository.allStores.removeObserver(localStoreObserver)
        }
        Log.d("DashboardViewModel", "=== CLEANUP COMPLETE ===")
    }

    // --- ALTE FUNCȚIONALITĂȚI (Favorite, User, Analytics) ---

    fun logViewStore(store: StoreModel) {
        val bundle = android.os.Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, store.getUniqueId())
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, store.Title)
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "store")
        bundle.putString("store_category", store.CategoryId)
        analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }

    fun getGlobalStoreList(): List<StoreModel> {
        return allStoresRaw
    }

    // User Profile
    private fun loadUserData() {
        userName.value = userManager.getName()
        userImagePath.value = userManager.getImagePath()
    }

    fun updateUserName(newName: String) {
        userName.value = newName
        userManager.saveName(newName)
    }

    fun updateUserImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val internalPath = userManager.copyImageToInternalStorage(uri)
            withContext(Dispatchers.Main) {
                if (internalPath != null) {
                    userImagePath.value = internalPath
                    userManager.saveImagePath(internalPath)
                }
            }
        }
    }

    // Favorites
    private fun loadFavorites() {
        favoriteStoreIds.clear()
        favoriteStoreIds.addAll(favoritesManager.getFavorites())
    }

    private fun updateFavoriteStores() {
        val favorites = allStoresRaw.filter { store ->
            favoriteStoreIds.contains(store.getUniqueId())
        }

        val sortedFavorites = favorites.sortedBy {
            if (it.distanceToUser < 0) Float.MAX_VALUE else it.distanceToUser
        }

        favoriteStores.clear()
        favoriteStores.addAll(sortedFavorites)
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

    // Loadere pentru Bannere și Categorii (prin Repository-ul vechi)
    fun loadCategory(): LiveData<MutableList<CategoryModel>> = dashboardRepository.loadCategory()
    fun loadBanner(): LiveData<MutableList<BannerModel>> = dashboardRepository.loadBanner()
}