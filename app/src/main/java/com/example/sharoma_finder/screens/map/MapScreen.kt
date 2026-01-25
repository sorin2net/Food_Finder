package com.example.sharoma_finder.screens.map

import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.sharoma_finder.R
import com.example.sharoma_finder.domain.StoreModel
import com.example.sharoma_finder.screens.results.ItemsNearest
import com.example.sharoma_finder.utils.LockScreenOrientation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@Composable
fun MapScreen(
    store: StoreModel,
    currentUserLocation: Location?,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onBackClick: () -> Unit
) {
    LockScreenOrientation()

    if (store.Latitude == 0.0 || store.Longitude == 0.0) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.black2)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Locația nu este disponibilă pentru acest restaurant\n\nTe rugăm să contactezi localul direct",
                fontSize = 18.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp)
            )
        }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storeLatlng = LatLng(store.Latitude, store.Longitude)

    val userLatLng = remember(currentUserLocation) {
        currentUserLocation?.let { LatLng(it.latitude, it.longitude) }
    }

    val storeMarkerState = remember(store.firebaseKey) { MarkerState(position = storeLatlng) }

    val userMarkerState = remember { MarkerState() }
    LaunchedEffect(userLatLng) {
        userLatLng?.let { userMarkerState.position = it }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(storeLatlng, 15f)
    }

    LaunchedEffect(store.firebaseKey) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(storeLatlng, 15f)
    }


    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (map, detail, backBtn, centerBtn) = createRefs()

        val mapProperties = remember {
            MapProperties(
                isMyLocationEnabled = false,
                mapType = MapType.NORMAL
            )
        }

        val uiSettings = remember {
            MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = true,
                compassEnabled = true
            )
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize().constrainAs(map) { centerTo(parent) },
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings
        ) {
            Marker(
                state = storeMarkerState,
                title = store.Title,
                snippet = store.Address,
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )

            userLatLng?.let {
                Marker(
                    state = userMarkerState,
                    title = "Locația ta",
                    snippet = "Ești aici",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(top = 48.dp, start = 16.dp)
                .size(45.dp)
                .background(colorResource(R.color.black3).copy(alpha = 0.8f), CircleShape)
                .clickable { onBackClick() }
                .constrainAs(backBtn) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.back),
                contentDescription = "Înapoi",
                modifier = Modifier.size(24.dp)
            )
        }

        userLatLng?.let { location ->
            Box(
                modifier = Modifier
                    .padding(top = 48.dp, end = 16.dp)
                    .size(45.dp)
                    .background(colorResource(R.color.black3).copy(alpha = 0.8f), CircleShape)
                    .clickable {
                        scope.launch {
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngZoom(location, 15f),
                                durationMs = 1000
                            )
                        }
                    }
                    .constrainAs(centerBtn) {
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Centrează pe mine",
                    tint = colorResource(R.color.gold),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .wrapContentHeight()
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
                .fillMaxWidth()
                .background(colorResource(R.color.black3), shape = RoundedCornerShape(10.dp))
                .padding(6.dp)
                .constrainAs(detail) {
                    centerHorizontallyTo(parent)
                    bottom.linkTo(parent.bottom)
                }
        ) {
            item {
                ItemsNearest(
                    item = store,
                    isFavorite = isFavorite,
                    onFavoriteClick = onFavoriteClick
                )
            }
            item {
                Button(
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.gold),
                        disabledContainerColor = Color.Gray
                    ),
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    enabled = store.hasValidPhoneNumber(),
                    onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${store.getCleanPhoneNumber()}"))
                        context.startActivity(dialIntent)
                    }
                ) {
                    Text(
                        text = if (store.hasValidPhoneNumber()) "Sună" else "Număr indisponibil",
                        fontSize = 16.sp,
                        color = if (store.hasValidPhoneNumber()) Color.Black else Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}