package com.example.sharoma_finder.screens.dashboard

import android.widget.Toast // ✅ Import necesar pentru Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.* // ✅ Importuri pentru animație
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh // ✅ Iconita de refresh standard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate // ✅ Modificator pentru rotație
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext // ✅ Context pentru Toast
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sharoma_finder.R
import com.example.sharoma_finder.viewModel.DashboardViewModel
import java.io.File

@Composable
fun ProfileScreen(viewModel: DashboardViewModel) {
    val context = LocalContext.current // ✅ Obținem contextul pentru Toast

    var showEditDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.updateUserImage(uri)
        }
    }

    // ✅ LOGICA PENTRU ANIMAȚIE
    // Creăm o tranziție infinită
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")

    // Calculăm unghiul de rotație doar dacă viewModel.isRefreshing este true
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "spin_angle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.black2)),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 64.dp)
        ) {
            // ... (Codul pentru Titlu, Poza de profil și Nume rămâne neschimbat) ...
            Text(
                text = "My Profile",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.gold),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // --- POZA DE PROFIL ---
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
            ) {
                if (viewModel.userImagePath.value != null) {
                    AsyncImage(
                        model = File(viewModel.userImagePath.value!!),
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.profile),
                        contentDescription = "Default Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_camera),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- NUMELE SI BUTONUL DE EDITARE ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = viewModel.userName.value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(onClick = {
                    tempName = viewModel.userName.value
                    showEditDialog = true
                }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Name",
                        tint = colorResource(R.color.gold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ✅ BUTONUL CU ANIMAȚIE ȘI TOAST
            Button(
                onClick = {
                    // Apelăm funcția din ViewModel și definim ce se întâmplă la final (onFinished)
                    viewModel.forceRefreshAllData {
                        Toast.makeText(context, "Everything refreshed! 🚀", Toast.LENGTH_SHORT).show()
                    }
                },
                // Dezactivăm butonul în timpul încărcării pentru a preveni dublu-click
                enabled = !viewModel.isRefreshing.value,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.gold),
                    disabledContainerColor = Color.DarkGray // Culoare când e dezactivat
                ),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Dacă se încarcă, folosim unghiul animat. Dacă nu, stă fix.
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = if (viewModel.isRefreshing.value) Color.Gray else Color.Black,
                        modifier = Modifier
                            .rotate(if (viewModel.isRefreshing.value) angle else 0f) // Aici aplicăm rotația
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (viewModel.isRefreshing.value) "Refreshing..." else "Force Refresh Data",
                        color = if (viewModel.isRefreshing.value) Color.Gray else Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Use this button to force download latest data from Firebase",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )
        }

        // ... (Codul pentru Dialogul de editare nume rămâne neschimbat) ...
        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                containerColor = colorResource(R.color.black3),
                title = { Text("Change Name", color = colorResource(R.color.gold)) },
                text = {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = colorResource(R.color.gold),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (tempName.isNotBlank()) {
                                viewModel.updateUserName(tempName)
                                showEditDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.gold))
                    ) {
                        Text("Save", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }
    }
}