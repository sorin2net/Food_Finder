package com.example.sharoma_finder.screens.results

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sharoma_finder.R
import com.example.sharoma_finder.domain.StoreModel
import com.example.sharoma_finder.repository.Resource
import com.example.sharoma_finder.viewModel.ResultsViewModel

@Composable
fun AllStoresScreen(
    categoryId: String,
    mode: String,
    onBackClick: () -> Unit,
    onStoreClick: (StoreModel) -> Unit,
    isStoreFavorite: (StoreModel) -> Boolean,
    onFavoriteToggle: (StoreModel) -> Unit,
    // Lista pre-calculată (deja sortată din ViewModel)
    preLoadedList: List<StoreModel>? = null,
    // ✅ ADĂUGAT: Locația utilizatorului pentru calcul distanță
    userLocation: Location? = null
) {
    val viewModel: ResultsViewModel = viewModel()

    // Alegem sursa datelor: Fie lista gata făcută, fie descărcăm de pe net
    val rawList = if (preLoadedList != null) {
        preLoadedList
    } else {
        val dataState = if (mode == "popular") {
            remember(categoryId) { viewModel.loadPopular(categoryId) }
        } else {
            remember(categoryId) { viewModel.loadNearest(categoryId) }
        }
        val resource by dataState.observeAsState(Resource.Loading())
        resource.data ?: emptyList()
    }

    // ✅ CALCULĂM DISTANȚA pentru toate magazinele dacă avem GPS
    val listToDisplay = remember(rawList, userLocation) {
        if (userLocation != null && rawList.isNotEmpty()) {
            rawList.map { store ->
                // Calculăm distanța pentru fiecare magazin
                val storeLoc = Location("store")
                storeLoc.latitude = store.Latitude
                storeLoc.longitude = store.Longitude

                // Clonăm obiectul și setăm distanța
                store.apply {
                    distanceToUser = userLocation.distanceTo(storeLoc)
                }
            }.sortedBy {
                if (it.distanceToUser < 0) Float.MAX_VALUE else it.distanceToUser
            }
        } else {
            rawList
        }
    }

    val isLoading = if (preLoadedList != null) false else listToDisplay.isEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.black2))
    ) {
        Column {
            TopTile(
                title = if (mode == "popular") "Popular" else "Nearest",
                onBackClick = onBackClick
            )

            if (isLoading && listToDisplay.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(listToDisplay.size) { index ->
                        val item = listToDisplay[index]
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // ✅ ItemsPopular afișează automat distanța (tu deja ai codul corect acolo)
                            ItemsPopular(
                                item = item,
                                isFavorite = isStoreFavorite(item),
                                onFavoriteClick = { onFavoriteToggle(item) },
                                onClick = { onStoreClick(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}