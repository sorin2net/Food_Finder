package com.example.sharoma_finder.screens.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    val lifecycleOwner = LocalLifecycleOwner.current

    val storeLatlng = LatLng(store.Latitude, store.Longitude)

    // ✅ FIX: Verificăm permisiunile LIVE
    var hasLocationPermission by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    // Camera centrată pe magazin
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(storeLatlng, 15f)
    }

    val storeMarkerState = remember { MarkerState(position = storeLatlng) }

    // ✅ FUNCȚIE HELPER: Verifică permisiunile
    fun checkPermissions(): Boolean {
        val fineLocation = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    // ✅ OBSERVER PENTRU LIFECYCLE (detectează când app-ul revine în foreground)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Când userul se întoarce în app, verificăm din nou permisiunile
                val currentPermission = checkPermissions()

                if (hasLocationPermission && !currentPermission) {
                    // Permisiunea a fost REVOCATĂ → Ștergem locația
                    Log.w("MapScreen", "⚠️ Location permission REVOKED - Removing marker")
                    userLocation = null
                }

                hasLocationPermission = currentPermission
                Log.d("MapScreen", "📍 Permission check on resume: $hasLocationPermission")
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ✅ TRACKING LOCAȚIE LIVE (cu verificare permisiuni)
    DisposableEffect(hasLocationPermission) {
        Log.d("MapScreen", "🗺️ MapScreen started")

        // Verificăm permisiunile la start
        val currentPermission = checkPermissions()
        hasLocationPermission = currentPermission

        if (!currentPermission) {
            Log.w("MapScreen", "❌ No location permissions - Blue marker will NOT appear")
            // ✅ FIX: Trebuie să returnăm onDispose chiar dacă nu facem nimic
            return@DisposableEffect onDispose { }
        }

        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // Update la fiecare 5 secunde
        ).apply {
            setMinUpdateIntervalMillis(2000L)
            setMaxUpdateDelayMillis(10000L)
        }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // ✅ RE-VERIFICĂM permisiunile la FIECARE update
                if (!checkPermissions()) {
                    Log.w("MapScreen", "⚠️ Permission lost during tracking - Stopping")
                    userLocation = null
                    fusedLocationClient.removeLocationUpdates(this)
                    return
                }

                locationResult.lastLocation?.let { location ->
                    userLocation = LatLng(location.latitude, location.longitude)
                    Log.d("MapScreen", "📍 Live update: ${location.latitude}, ${location.longitude}")
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
            Log.d("MapScreen", "✅ Location tracking STARTED")
        } catch (e: SecurityException) {
            Log.e("MapScreen", "❌ Security exception: ${e.message}")
        }

        onDispose {
            Log.d("MapScreen", "🧹 Cleaning up location tracking")
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                userLocation = null
                System.gc()
                Log.d("MapScreen", "✅ Cleanup complete")
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
            // ✅ MARKER 1: MAGAZINUL (Roșu - ÎNTOTDEAUNA vizibil)
            Marker(
                state = storeMarkerState,
                title = store.Title,
                snippet = store.Address,
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )

            // ✅ MARKER 2: UTILIZATORUL (Albastru - DOAR dacă are permisiune ȘI locație)
            if (hasLocationPermission && userLocation != null) {
                // ✅ FIX: Folosim remember(userLocation) pentru a crea state-ul.
                // Asta elimină warning-ul și asigură că marker-ul se mută doar când locația se schimbă.
                val userMarkerState = remember(userLocation) {
                    MarkerState(position = userLocation!!)
                }

                Marker(
                    state = userMarkerState,
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