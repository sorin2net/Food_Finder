package com.example.sharoma_finder.screens.results

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
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

@Composable
fun ResultList(
    id: String,
    title: String,
    onBackClick: () -> Unit,
    onStoreClick: (StoreModel) -> Unit,
    onSeeAllClick: (String) -> Unit,
    isStoreFavorite: (StoreModel) -> Boolean,
    onFavoriteToggle: (StoreModel) -> Unit,
    // Aceasta conține TOATE magazinele cu distanța deja calculată în Dashboard
    allGlobalStores: List<StoreModel> = emptyList(),
    userLocation: Location? = null
) {
    val viewModel: ResultsViewModel = viewModel()

    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedCategoryName by remember { mutableStateOf("") }

    // Încărcăm doar subcategoriile (Burger, Pizza etc)
    val subCategoryState by remember(id) { viewModel.loadSubCategory(id) }.observeAsState(Resource.Loading())
    val subCategoryList = subCategoryState.data ?: emptyList()
    val showSubCategoryLoading = subCategoryState is Resource.Loading
    val subCategorySnapshot = remember(subCategoryList) { listToSnapshot(subCategoryList) }

    // --- FILTRARE DIN LISTA GLOBALĂ ---
    // În loc să descărcăm din nou, filtrăm lista globală pentru categoria curentă (id)

    // 1. Lista Popular pentru categoria curentă
    val categoryPopularList = remember(allGlobalStores, id) {
        allGlobalStores.filter {
            it.CategoryId == id && it.IsPopular
        }
    }

    // 2. Lista Nearest pentru categoria curentă (sortată după distanță)
    val categoryNearestList = remember(allGlobalStores, id, userLocation) {
        val filtered = allGlobalStores.filter { it.CategoryId == id }
        // Sortăm doar dacă avem distanțe valide
        if (userLocation != null) {
            filtered.sortedBy { if (it.distanceToUser < 0) Float.MAX_VALUE else it.distanceToUser }
        } else {
            filtered
        }
    }

    // Convertim în snapshot pentru UI
    val popularSnapshot = remember(categoryPopularList) { listToSnapshot(categoryPopularList) }
    val nearestSnapshot = remember(categoryNearestList) { listToSnapshot(categoryNearestList) }

    // --- LOGICA DE CĂUTARE (Căutăm în TOT, nu doar în categorie) ---
    val searchResults = remember(searchText, allGlobalStores) {
        if (searchText.isEmpty()) {
            emptyList()
        } else {
            allGlobalStores
                .filter { store ->
                    store.Title.contains(searchText, ignoreCase = true) ||
                            store.Address.contains(searchText, ignoreCase = true)
                }
                .sortedBy {
                    if (it.distanceToUser < 0) Float.MAX_VALUE else it.distanceToUser
                }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.black2))
    ) {
        item { TopTile(title, onBackClick) }

        item {
            Search(
                text = searchText,
                onValueChange = { newText -> searchText = newText }
            )
        }

        if (searchText.isNotEmpty()) {
            // --- REZULTATE CĂUTARE ---
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
                item {
                    val rows = searchResults.chunked(2)
                    Column(Modifier.padding(16.dp)) {
                        rows.forEach { rowItems ->
                            Row(
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
                                if (rowItems.size < 2) Spacer(modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

        } else {
            // --- LISTELE STANDARD ---

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

            // Filtrare locală pe baza sub-categoriei selectate (ex: Burger, Pizza)
            val filteredPopular = if (selectedCategoryName.isEmpty()) popularSnapshot else {
                val filtered = categoryPopularList.filter { it.Activity.equals(selectedCategoryName, ignoreCase = true) }
                listToSnapshot(filtered)
            }

            val filteredNearest = if (selectedCategoryName.isEmpty()) nearestSnapshot else {
                val filtered = categoryNearestList.filter { it.Activity.equals(selectedCategoryName, ignoreCase = true) }
                listToSnapshot(filtered)
            }

            // Afișăm secțiunile doar dacă avem date
            item {
                if (filteredPopular.isNotEmpty()) {
                    PopularSection(
                        list = filteredPopular,
                        showPopularLoading = false, // Avem deja datele
                        onStoreClick = onStoreClick,
                        onSeeAllClick = { onSeeAllClick("popular") },
                        isStoreFavorite = isStoreFavorite,
                        onFavoriteToggle = onFavoriteToggle
                    )
                }
            }

            item {
                if (filteredNearest.isNotEmpty()) {
                    NearestList(
                        list = filteredNearest,
                        showNearestLoading = false, // Avem deja datele
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