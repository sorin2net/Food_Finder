package com.example.sharoma_finder.screens.results

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.example.sharoma_finder.R
import com.example.sharoma_finder.domain.StoreModel

@Composable
fun AllStoresScreen(
    categoryId: String,
    mode: String,
    onBackClick: () -> Unit,
    onStoreClick: (StoreModel) -> Unit,
    isStoreFavorite: (StoreModel) -> Boolean,
    onFavoriteToggle: (StoreModel) -> Unit,
    // ✅ Lista deja pregătită în DashboardViewModel
    preLoadedList: List<StoreModel>? = null,
    userLocation: Location? = null
) {
    // ✅ ELIMINAT: Nu mai folosim ResultsViewModel
    // Toate datele vin prin preLoadedList (deja calculate în DashboardViewModel)

    val listToDisplay = remember(preLoadedList, userLocation) {
        if (preLoadedList != null && userLocation != null) {
            // Sortăm din nou pentru siguranță (deja ar trebui sortate, dar e fail-safe)
            preLoadedList.sortedBy {
                if (it.distanceToUser < 0) Float.MAX_VALUE else it.distanceToUser
            }
        } else {
            preLoadedList ?: emptyList()
        }
    }

    val isLoading = listToDisplay.isEmpty()

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

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colorResource(R.color.gold))
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