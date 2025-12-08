package com.example.sharoma_finder.screens.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items // Important pentru Grid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sharoma_finder.R
import com.example.sharoma_finder.domain.StoreModel
import com.example.sharoma_finder.repository.Resource
import com.example.sharoma_finder.viewModel.ResultsViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

@Composable
fun ResultList(
    id: String,
    title: String,
    onBackClick: () -> Unit,
    onStoreClick: (StoreModel) -> Unit,
    onSeeAllClick: (String) -> Unit,
    isStoreFavorite: (StoreModel) -> Boolean,
    onFavoriteToggle: (StoreModel) -> Unit
) {
    val viewModel: ResultsViewModel = viewModel()

    // 1. Starea textului de căutare este acum aici, în părinte
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedCategoryName by remember { mutableStateOf("") }

    val subCategoryState by remember(id) { viewModel.loadSubCategory(id) }.observeAsState(Resource.Loading())
    val popularState by remember(id) { viewModel.loadPopular(id, limit = 100) }.observeAsState(Resource.Loading())
    val nearestState by remember(id) { viewModel.loadNearest(id, limit = 100) }.observeAsState(Resource.Loading())

    val subCategoryList = subCategoryState.data ?: emptyList()
    val popularList = popularState.data ?: emptyList()
    val nearestList = nearestState.data ?: emptyList()

    val showSubCategoryLoading = subCategoryState is Resource.Loading
    val showPopularLoading = popularState is Resource.Loading
    val showNearestLoading = nearestState is Resource.Loading

    // Convertim în snapshot pentru UI-ul vechi
    val subCategorySnapshot = remember(subCategoryList) { listToSnapshot(subCategoryList) }
    val popularSnapshot = remember(popularList) { listToSnapshot(popularList) }
    val nearestSnapshot = remember(nearestList) { listToSnapshot(nearestList) }

    // 2. Logică pentru CĂUTARE GLOBALĂ (combinăm listele și eliminăm duplicatele)
    val allCombinedStores = remember(popularList, nearestList) {
        (popularList + nearestList).distinctBy { it.getUniqueId() }
    }

    // 3. Logică de filtrare bazată pe textul scris
    val searchResults = remember(searchText, allCombinedStores) {
        if (searchText.isEmpty()) {
            emptyList()
        } else {
            allCombinedStores.filter { store ->
                store.Title.contains(searchText, ignoreCase = true) ||
                        store.Address.contains(searchText, ignoreCase = true)
            }
        }
    }

    // Design-ul paginii
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.black2))
    ) {
        item { TopTile(title, onBackClick) }

        // 4. Conectăm bara de search la variabila locală searchText
        item {
            Search(
                text = searchText,
                onValueChange = { newText -> searchText = newText }
            )
        }

        // 5. LOGICA DE AFIȘARE: Dacă avem text în search, arătăm rezultatele. Altfel, arătăm ecranul normal.
        if (searchText.isNotEmpty()) {
            item {
                Text(
                    text = "Search Results (${searchResults.size})",
                    color = colorResource(R.color.gold),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (searchResults.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No stores found matching \"$searchText\"", color = Color.Gray)
                    }
                }
            } else {
                // Afișăm rezultatele ca un Grid (două pe rând)
                // Folosim un truc pentru a pune Grid-ul într-un LazyColumn item
                item {
                    // Calculăm înălțimea necesară sau folosim un flow layout
                    // Dar cea mai simplă metodă într-un LazyColumn este să afișăm rând cu rând manual sau VerticalGrid
                    // Aici folosim flow-ul manual pentru a nu avea erori de nested scrolling
                    val rows = searchResults.chunked(2)
                    Column(Modifier.padding(16.dp)) {
                        rows.forEach { rowItems ->
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                rowItems.forEach { store ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        ItemsPopular(
                                            item = store,
                                            isFavorite = isStoreFavorite(store),
                                            onFavoriteClick = { onFavoriteToggle(store) },
                                            onClick = { onStoreClick(store) }
                                        )
                                    }
                                }
                                // Dacă e număr impar, adăugăm un spațiu gol
                                if (rowItems.size < 2) {
                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

        } else {
            // --- ECRANUL ORIGINAL (Subcategorii, Popular, Nearest) ---

            item {
                SubCategory(
                    subCategory = subCategorySnapshot,
                    showSubCategoryLoading = showSubCategoryLoading,
                    selectedCategoryName = selectedCategoryName,
                    onCategoryClick = { clickedName ->
                        selectedCategoryName = if (selectedCategoryName == clickedName) "" else clickedName
                    }
                )
            }


            val filteredPopular = if (selectedCategoryName.isEmpty()) popularSnapshot else listToSnapshot(popularList.filter { it.Activity.equals(selectedCategoryName, ignoreCase = true) })
            val filteredNearest = if (selectedCategoryName.isEmpty()) nearestSnapshot else listToSnapshot(nearestList.filter { it.Activity.equals(selectedCategoryName, ignoreCase = true) })

            item {
                if (!showPopularLoading && filteredPopular.isEmpty()) {

                } else {
                    PopularSection(
                        list = filteredPopular,
                        showPopularLoading = showPopularLoading,
                        onStoreClick = onStoreClick,
                        onSeeAllClick = { onSeeAllClick("popular") },
                        isStoreFavorite = isStoreFavorite,
                        onFavoriteToggle = onFavoriteToggle
                    )
                }
            }

            item {
                if (!showNearestLoading && filteredNearest.isEmpty()) {

                } else {
                    NearestList(
                        list = filteredNearest,
                        showNearestLoading = showNearestLoading,
                        onStoreClick = onStoreClick,
                        onSeeAllClick = { onSeeAllClick("nearest") },
                        isStoreFavorite = isStoreFavorite,
                        onFavoriteToggle = onFavoriteToggle
                    )
                }
            }
        }
    }
}

fun <T> listToSnapshot(list: List<T>): SnapshotStateList<T> {
    val snapshot = androidx.compose.runtime.mutableStateListOf<T>()
    snapshot.addAll(list)
    return snapshot
}