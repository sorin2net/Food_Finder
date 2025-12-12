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
import com.example.sharoma_finder.screens.common.ErrorScreen

@Composable
fun ResultList(
    id: String,
    title: String,
    onBackClick: () -> Unit,
    onStoreClick: (StoreModel) -> Unit,
    onSeeAllClick: (String) -> Unit,
    isStoreFavorite: (StoreModel) -> Boolean,
    onFavoriteToggle: (StoreModel) -> Unit,
    allGlobalStores: List<StoreModel> = emptyList(),
    userLocation: Location? = null
) {
    val viewModel: ResultsViewModel = viewModel()

    var searchText by rememberSaveable { mutableStateOf("") }

    // ✅ MODIFICAT: Acum stocăm TAG-ul selectat în loc de nume subcategorie
    var selectedTag by remember { mutableStateOf("") }

    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Subcategories (acestea rămân la fel - Burger, Pizza, Sushi, etc.)
    val subCategoryState by remember(id) {
        viewModel.loadSubCategory(id)
    }.observeAsState(Resource.Loading())

    val subCategoryList = when (subCategoryState) {
        is Resource.Success -> subCategoryState.data ?: emptyList()
        is Resource.Error -> {
            LaunchedEffect(Unit) {
                hasError = true
                errorMessage = subCategoryState.message ?: "Failed to load categories"
            }
            emptyList()
        }
        else -> emptyList()
    }

    val showSubCategoryLoading = subCategoryState is Resource.Loading
    val subCategorySnapshot = remember(subCategoryList) { listToSnapshot(subCategoryList) }

    // ✅ MODIFICAT: Filtrăm după CategoryId și după Tag-uri (dacă e selectat un tag)
    val categoryPopularList = remember(allGlobalStores, id, selectedTag) {
        try {
            allGlobalStores.filter { store ->
                val matchesCategory = store.CategoryId == id && store.IsPopular && store.isValid()

                // Dacă nu e selectat niciun tag, arată toate magazinele din categorie
                if (selectedTag.isEmpty()) {
                    matchesCategory
                } else {
                    // Dacă e selectat un tag, arată doar magazinele care au acel tag
                    matchesCategory && store.hasTag(selectedTag)
                }
            }
        } catch (e: Exception) {
            hasError = true
            errorMessage = "Error filtering popular stores: ${e.message}"
            emptyList()
        }
    }

    // ✅ MODIFICAT: Același lucru pentru Nearest
    val categoryNearestList = remember(allGlobalStores, id, userLocation, selectedTag) {
        try {
            val filtered = allGlobalStores.filter { store ->
                val matchesCategory = store.CategoryId == id && store.isValid()

                if (selectedTag.isEmpty()) {
                    matchesCategory
                } else {
                    matchesCategory && store.hasTag(selectedTag)
                }
            }

            if (userLocation != null) {
                filtered.sortedBy {
                    if (it.distanceToUser < 0) Float.MAX_VALUE else it.distanceToUser
                }
            } else {
                filtered
            }
        } catch (e: Exception) {
            hasError = true
            errorMessage = "Error filtering nearest stores: ${e.message}"
            emptyList()
        }
    }

    val popularSnapshot = remember(categoryPopularList) { listToSnapshot(categoryPopularList) }
    val nearestSnapshot = remember(categoryNearestList) { listToSnapshot(categoryNearestList) }

    // ✅ MODIFICAT: Search caută și în Tag-uri
    val searchResults = remember(searchText, allGlobalStores) {
        if (searchText.isEmpty()) {
            emptyList()
        } else {
            try {
                allGlobalStores
                    .filter { store ->
                        store.isValid() && (
                                store.Title.contains(searchText, ignoreCase = true) ||
                                        store.Address.contains(searchText, ignoreCase = true) ||
                                        // ✅ NOU: Caută și în tag-uri
                                        store.Tags.any { tag ->
                                            tag.contains(searchText, ignoreCase = true)
                                        }
                                )
                    }
                    .sortedBy {
                        if (it.distanceToUser < 0) Float.MAX_VALUE else it.distanceToUser
                    }
            } catch (e: Exception) {
                hasError = true
                errorMessage = "Search error: ${e.message}"
                emptyList()
            }
        }
    }

    if (hasError) {
        ErrorScreen(
            message = errorMessage,
            onRetry = {
                hasError = false
                errorMessage = ""
            }
        )
        return
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
            // Search Results
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No stores found matching \"$searchText\"",
                            color = Color.Gray
                        )
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
            // ✅ MODIFICAT: Când apeși pe subcategorie (Burger, Pizza, etc.),
            // setăm selectedTag în loc de selectedCategoryName
            item {
                SubCategory(
                    subCategory = subCategorySnapshot,
                    showSubCategoryLoading = showSubCategoryLoading,
                    selectedCategoryName = selectedTag, // Folosim același parametru
                    onCategoryClick = { clickedTag ->
                        // Dacă dai click pe același tag, îl deselectezi (arată tot)
                        selectedTag = if (selectedTag == clickedTag) "" else clickedTag
                    }
                )
            }

            // ✅ IMPORTANT: Nu mai facem filtrare aici, o facem mai sus în remember()
            item {
                if (popularSnapshot.isNotEmpty()) {
                    PopularSection(
                        list = popularSnapshot,
                        showPopularLoading = false,
                        onStoreClick = onStoreClick,
                        onSeeAllClick = { onSeeAllClick("popular") },
                        isStoreFavorite = isStoreFavorite,
                        onFavoriteToggle = onFavoriteToggle
                    )
                } else if (selectedTag.isNotEmpty()) {
                    // Mesaj când nu există restaurante cu tag-ul selectat
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No popular stores found with tag \"$selectedTag\"",
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            item {
                if (nearestSnapshot.isNotEmpty()) {
                    NearestList(
                        list = nearestSnapshot,
                        showNearestLoading = false,
                        onStoreClick = onStoreClick,
                        onSeeAllClick = { onSeeAllClick("nearest") },
                        isStoreFavorite = isStoreFavorite,
                        onFavoriteToggle = onFavoriteToggle
                    )
                } else if (selectedTag.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No nearby stores found with tag \"$selectedTag\"",
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
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