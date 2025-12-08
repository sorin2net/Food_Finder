package com.example.sharoma_finder.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sharoma_finder.R
import com.example.sharoma_finder.domain.StoreModel
import com.example.sharoma_finder.screens.results.ItemsNearest

@Composable
fun SupportScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Support Screen", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colorResource(R.color.gold))
    }
}

@Composable
fun ProfileScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Profile Screen", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colorResource(R.color.gold))
    }
}

@Composable
fun WishlistScreen(
    favoriteStores: List<StoreModel>,
    onFavoriteToggle: (Int) -> Unit,
    onStoreClick: (StoreModel) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.black2))
    ) {
        if (favoriteStores.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Your wishlist is empty", color = Color.Gray, fontSize = 18.sp)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        text = "My Wishlist",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.gold),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                items(favoriteStores.size) { index ->
                    val store = favoriteStores[index]

                    ItemsNearest(
                        item = store,
                        isFavorite = true, // Aici sunt mereu favorite
                        onFavoriteClick = { onFavoriteToggle(store.Id) },
                        onClick = { onStoreClick(store) }
                    )
                }
            }
        }
    }
}