package com.example.sharoma_finder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.viewmodel.compose.viewModel // Asigură-te că ai importul acesta
import com.example.sharoma_finder.domain.StoreModel
import com.example.sharoma_finder.screens.dashboard.DashboardScreen
import com.example.sharoma_finder.screens.map.MapScreen
import com.example.sharoma_finder.screens.results.AllStoresScreen
import com.example.sharoma_finder.screens.results.ResultList
import com.example.sharoma_finder.viewModel.DashboardViewModel // Importul ViewModel-ului
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainApp()
        }
    }
}

sealed class Screen {
    data object Dashboard : Screen()
    data class Results(val id: String, val title: String) : Screen()
    data class Map(val store: StoreModel) : Screen()
    data class ViewAll(val id: String, val mode: String) : Screen()
}

@Composable
fun MainApp() {
    val systemUiController = rememberSystemUiController()
    systemUiController.setStatusBarColor(color = colorResource(R.color.white))


    val dashboardViewModel: DashboardViewModel = viewModel()

    val backStack = remember { mutableStateListOf<Screen>(Screen.Dashboard) }
    val currentScreen = backStack.last()

    fun popBackStack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        popBackStack()
    }

    when (val screen = currentScreen) {
        Screen.Dashboard -> {
            DashboardScreen(
                onCategoryClick = { id, title ->
                    backStack.add(Screen.Results(id, title))
                },
                onStoreClick = { store ->
                    backStack.add(Screen.Map(store))
                }
            )
        }
        is Screen.Results -> {
            ResultList(
                id = screen.id,
                title = screen.title,
                onBackClick = { popBackStack() },
                onStoreClick = { store -> backStack.add(Screen.Map(store)) },
                onSeeAllClick = { mode ->
                    backStack.add(Screen.ViewAll(screen.id, mode))
                },
                // NOTĂ: Dacă vrei ca și aici (în lista scurtă) să meargă inima,
                // trebuie să actualizezi și ResultList să accepte parametrii isStoreFavorite/onFavoriteToggle
                // momentan l-am lăsat așa ca să nu îți dea eroare dacă nu ai modificat ResultList.
            )
        }
        is Screen.ViewAll -> {
            AllStoresScreen(
                categoryId = screen.id,
                mode = screen.mode,
                onBackClick = { popBackStack() },
                onStoreClick = { store -> backStack.add(Screen.Map(store)) },

                isStoreFavorite = { id -> dashboardViewModel.isFavorite(id) },
                onFavoriteToggle = { id -> dashboardViewModel.toggleFavorite(id) }
            )
        }
        is Screen.Map -> {
            MapScreen(
                store = screen.store,
                isFavorite = dashboardViewModel.isFavorite(screen.store.Id),
                onFavoriteClick = { dashboardViewModel.toggleFavorite(screen.store.Id) }
            )
        }
    }
}