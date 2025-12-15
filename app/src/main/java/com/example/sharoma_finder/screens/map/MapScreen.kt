package com.example.sharoma_finder.screens.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log // ✅ ADĂUGAT PENTRU LOGGING
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.app.ActivityCompat
import com.example.sharoma_finder.R
import com.example.sharoma_finder.domain.StoreModel
import com.example.sharoma_finder.screens.results.ItemsNearest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MapScreen(
    store: StoreModel,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {}
) {
    // ✅ Validare coordonate magazin
    if (store.Latitude == 0.0 || store.Longitude == 0.0) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.black2)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Location not available for this store\n\nPlease contact the store directly",
                fontSize = 18.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp)
            )
        }
        return
    }

    val context = LocalContext.current

    // Poziția magazinului
    val storeLatlng = LatLng(store.Latitude, store.Longitude)

    // State pentru locația utilizatorului (LIVE)
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    // Camera inițial centrată pe magazin
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(storeLatlng, 15f)
    }

    // Marker state pentru magazin (fix)
    val storeMarkerState = remember { MarkerState(position = storeLatlng) }

    // ✅ TRACKING LIVE AL LOCAȚIEI UTILIZATORULUI (CU MEMORY LEAK FIX)
    DisposableEffect(Unit) {
        Log.d("MapScreen", "🗺️ MapScreen started - Setting up location tracking")

        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        // Cerere de locație cu update-uri frecvente
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, // Precizie maximă
            5000L // Update la fiecare 5 secunde
        ).apply {
            setMinUpdateIntervalMillis(2000L) // Minim 2 secunde între update-uri
            setMaxUpdateDelayMillis(10000L) // Maxim 10 secunde delay
        }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // ✅ Actualizăm poziția utilizatorului LIVE
                    userLocation = LatLng(location.latitude, location.longitude)
                    Log.d("MapScreen", "📍 Location updated: ${location.latitude}, ${location.longitude}")
                }
            }
        }

        // Verificăm permisiunile
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                // Pornim tracking-ul live
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null // Main Looper
                )
                Log.d("MapScreen", "✅ Location updates started")
            } catch (e: SecurityException) {
                Log.e("MapScreen", "❌ Security exception: ${e.message}")
            }
        } else {
            Log.w("MapScreen", "⚠️ No location permissions")
        }

        // ✅ CLEANUP COMPLET când părăsim ecranul
        onDispose {
            Log.d("MapScreen", "🧹 MapScreen disposed - Cleaning up resources")
            try {
                // 1. Oprește location updates
                fusedLocationClient.removeLocationUpdates(locationCallback)
                Log.d("MapScreen", "✅ Location updates stopped")

                // 2. Curăță referințele pentru a preveni memory leak
                userLocation = null

                // 3. Forțează garbage collection hint (opțional, dar bun pentru siguranță aici)
                System.gc()
            } catch (e: Exception) {
                Log.e("MapScreen", "❌ Cleanup error: ${e.message}")
            }
        }
    }

    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val (map, detail) = createRefs()

        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .constrainAs(map) {
                    centerTo(parent)
                },
            cameraPositionState = cameraPositionState
        ) {
            // ✅ MARKER 1: MAGAZINUL (PIN ROȘU)
            Marker(
                state = storeMarkerState,
                title = store.Title,
                snippet = store.Address,
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )

            // ✅ MARKER 2: UTILIZATORUL (PIN ALBASTRU - se mișcă live)
            userLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    title = "Your Location",
                    snippet = "You are here",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                )
            }
        }

        // Card cu detalii magazin
        LazyColumn(
            modifier = Modifier
                .wrapContentHeight()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .fillMaxWidth()
                .background(colorResource(R.color.black3), shape = RoundedCornerShape(10.dp))
                .padding(16.dp)
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
                        containerColor = colorResource(R.color.gold)
                    ),
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    onClick = {
                        val phoneNumber = "tel:" + store.Call
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse(phoneNumber))
                        context.startActivity(dialIntent)
                    }
                ) {
                    Text(
                        "Call to Store",
                        fontSize = 18.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}