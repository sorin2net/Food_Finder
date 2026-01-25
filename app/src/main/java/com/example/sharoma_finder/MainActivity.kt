package com.example.sharoma_finder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sharoma_finder.domain.StoreModel
import com.example.sharoma_finder.repository.InternetConsentManager
import com.example.sharoma_finder.screens.common.InternetConsentDialog
import com.example.sharoma_finder.screens.dashboard.DashboardScreen
import com.example.sharoma_finder.screens.map.MapScreen
import com.example.sharoma_finder.screens.random.RandomRecommenderScreen
import com.example.sharoma_finder.screens.results.AllStoresScreen
import com.example.sharoma_finder.screens.results.ResultList
import com.example.sharoma_finder.viewModel.DashboardViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.parcelize.Parcelize

class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private lateinit var internetConsentManager: InternetConsentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        internetConsentManager = InternetConsentManager(applicationContext)

        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineLocation = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
            val coarseLocation = permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)

            if (fineLocation || coarseLocation) {
                dashboardViewModel.fetchUserLocation()
                dashboardViewModel.checkLocationPermission()
                dashboardViewModel.startLocationUpdates()
            } else {
                dashboardViewModel.checkLocationPermission()
            }
        }

        val hasFineLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation || hasCoarseLocation) {
            dashboardViewModel.fetchUserLocation()
            dashboardViewModel.startLocationUpdates()
        } else {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }

        setContent {
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            dashboardViewModel.startUsageTimer()
                            dashboardViewModel.checkLocationPermission()

                            val context = this@MainActivity
                            val hasFine = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (hasFine || hasCoarse) {
                                dashboardViewModel.startLocationUpdates()
                            }
                        }
                        Lifecycle.Event.ON_PAUSE -> {
                            dashboardViewModel.stopUsageTimer()
                            dashboardViewModel.stopLocationUpdates()
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            MainApp(
                dashboardViewModel = dashboardViewModel,
                internetConsentManager = internetConsentManager
            )
        }
    }
}

@Parcelize
sealed class Screen : Parcelable {
    @Parcelize
    object Dashboard : Screen()

    @Parcelize
    data class Results(val id: String, val title: String) : Screen()

    @Parcelize
    data class Map(
        val storeFirebaseKey: String,
        val storeTitle: String,
        val latitude: Double,
        val longitude: Double
    ) : Screen()

    @Parcelize
    data class ViewAll(val id: String, val mode: String) : Screen()

    @Parcelize
    object RandomRecommender : Screen()
}

val BackStackSaver = listSaver<SnapshotStateList<Screen>, Screen>(
    save = { stateList ->
        stateList.toList()
    },
    restore = { savedList ->
        SnapshotStateList<Screen>().apply {
            addAll(savedList)
        }
    }
)

@Composable
fun MainApp(
    dashboardViewModel: DashboardViewModel,
    internetConsentManager: InternetConsentManager
) {
    val systemUiController = rememberSystemUiController()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        systemUiController.isNavigationBarVisible = false
        systemUiController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    systemUiController.setStatusBarColor(color = colorResource(R.color.white))

    val backStack = rememberSaveable(
        saver = BackStackSaver
    ) {
        SnapshotStateList<Screen>().apply {
            add(Screen.Dashboard)
        }
    }

    val currentScreen = backStack.lastOrNull() ?: Screen.Dashboard

    var showInternetConsentDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!internetConsentManager.hasAskedForConsent() && !internetConsentManager.hasInternetConsent()) {
            showInternetConsentDialog = true
        } else if (internetConsentManager.hasInternetConsent()) {
            dashboardViewModel.enableInternetFeatures()
        }
    }

    if (showInternetConsentDialog) {
        InternetConsentDialog(
            onAccept = {
                internetConsentManager.grantConsent()
                dashboardViewModel.enableInternetFeatures()
                showInternetConsentDialog = false
            },
            onDecline = {
                internetConsentManager.markConsentAsked()
                dashboardViewModel.disableInternetFeatures()
                showInternetConsentDialog = false
            }
        )
    }

    fun popBackStack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        popBackStack()
    }

    val navigateToMap: (StoreModel) -> Unit = { store ->
        dashboardViewModel.onStoreOpenedOnMap()
        backStack.add(
            Screen.Map(
                storeFirebaseKey = store.firebaseKey,
                storeTitle = store.Title,
                latitude = store.Latitude,
                longitude = store.Longitude
            )
        )
    }

    when (val screen = currentScreen) {
        Screen.Dashboard -> {
            DashboardScreen(
                onCategoryClick = { id, title ->
                    backStack.add(Screen.Results(id, title))
                },
                onStoreClick = navigateToMap,
                onBannerClick = {
                    backStack.add(Screen.RandomRecommender)
                },
                viewModel = dashboardViewModel
            )
        }

        Screen.RandomRecommender -> {
            RandomRecommenderScreen(
                allStores = dashboardViewModel.liveAllStores,
                viewModel = dashboardViewModel,
                onBackClick = { popBackStack() },
                onStoreClick = navigateToMap
            )
        }

        is Screen.Results -> {

            ResultList(
                id = screen.id,
                title = screen.title,
                onBackClick = { popBackStack() },
                onStoreClick = navigateToMap,
                onSeeAllClick = { mode ->
                    backStack.add(Screen.ViewAll(screen.id, mode))
                },
                isStoreFavorite = { store -> dashboardViewModel.isFavorite(store) },
                onFavoriteToggle = { store -> dashboardViewModel.toggleFavorite(store) },
                allGlobalStores = dashboardViewModel.liveAllStores,
                userLocation = dashboardViewModel.currentUserLocation,
                lastUpdateTick = dashboardViewModel.lastCalculationTimestamp
            )
        }

        is Screen.ViewAll -> {
            val updateTick = dashboardViewModel.lastCalculationTimestamp
            val liveFilteredList by remember(screen.mode, screen.id, dashboardViewModel.liveAllStores,updateTick) {
                derivedStateOf {
                    val list = dashboardViewModel.liveAllStores
                    when (screen.mode) {
                        "popular" -> {
                            list.filter { it.CategoryIds.contains(screen.id) && it.IsPopular }
                                .sortedBy { if (it.distanceToUser < 0) Float.MAX_VALUE else it.distanceToUser }
                        }
                        "nearest", "nearest_all" -> {
                            list.filter { it.CategoryIds.contains(screen.id) }
                                .sortedBy { if (it.distanceToUser < 0) Float.MAX_VALUE else it.distanceToUser }
                        }
                        else -> emptyList()
                    }
                }
            }

            AllStoresScreen(
                categoryId = screen.id,
                mode = screen.mode,
                onBackClick = { popBackStack() },
                onStoreClick = navigateToMap,
                isStoreFavorite = { store -> dashboardViewModel.isFavorite(store) },
                onFavoriteToggle = { store -> dashboardViewModel.toggleFavorite(store) },
                preLoadedList = liveFilteredList,
                userLocation = dashboardViewModel.currentUserLocation
            )
        }

        is Screen.Map -> {
            val liveStore = dashboardViewModel.liveAllStores
                .firstOrNull { it.firebaseKey == screen.storeFirebaseKey }

            if (liveStore != null) {
                MapScreen(
                    store = liveStore,
                    isFavorite = dashboardViewModel.isFavorite(liveStore),
                    onFavoriteClick = { dashboardViewModel.toggleFavorite(liveStore) },
                    onBackClick = { popBackStack() }
                )
            } else {
                val tempStore = StoreModel(
                    firebaseKey = screen.storeFirebaseKey,
                    Title = screen.storeTitle,
                    Latitude = screen.latitude,
                    Longitude = screen.longitude
                )
                MapScreen(
                    store = tempStore,
                    isFavorite = false,
                    onFavoriteClick = {},
                    onBackClick = { popBackStack() }
                )
            }
        }
    }
}