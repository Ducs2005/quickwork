package com.example.quickwork.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CheckboxDefaults.colors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.quickwork.data.models.Address
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@SuppressLint("ClickableViewAccessibility")
@Composable
fun MapPickerScreen(navController: NavController) {
    val backgroundColor = Color(0xFFB3E5FC) // Light blue for header
    val buttonColor = Color(0xFF81C784) // Green for buttons

    // Initial location: Ho Chi Minh City
    val initialPosition = GeoPoint(10.7769, 106.7009)
    var selectedPosition by remember { mutableStateOf<GeoPoint?>(null) }
    var selectedAddress by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Initialize OSMDroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = "QuickWorkApp/1.0" // Required for Nominatim
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        }
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            Text(
                text = "Select Address",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Map
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(15.0)
                            controller.setCenter(initialPosition)
                        }
                    },
                    update = { mapView: MapView -> // Explicitly define the type of mapView
                        mapView.setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_UP) {
                                val projection = mapView.projection
                                val geoPoint = projection.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
                                selectedPosition = geoPoint
                                coroutineScope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    try {
                                        selectedAddress = getAddressFromLatLng(geoPoint.latitude, geoPoint.longitude)
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to fetch address: ${e.message}"
                                        selectedAddress = ""
                                    } finally {
                                        isLoading = false
                                    }
                                }

                            }
                            false // Allow default map interactions
                        }
                    }
                )

            }

            // Address and Error
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = buttonColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (selectedAddress.isNotEmpty()) {
                    Text(
                        text = "Selected: $selectedAddress",
                        fontSize = 14.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Coordinates: (${selectedPosition?.latitude}, ${selectedPosition?.longitude})",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Confirm Button
            Button(
                onClick = {
                    selectedPosition?.let { pos ->
                        if (selectedAddress.isNotEmpty()) {
                            val address = Address(
                                latitude = pos.latitude,
                                longitude = pos.longitude,
                                address = selectedAddress,
                                timestamp = System.currentTimeMillis()
                            )
                            navController.previousBackStackEntry?.savedStateHandle?.set(
                                "selectedAddress",
                                Json.encodeToString(address)
                            )
                            navController.popBackStack()
                        } else {
                            errorMessage = "Please select a valid address"
                        }
                    } ?: run {
                        errorMessage = "Please select a location on the map"
                    }
                },
                shape = RoundedCornerShape(12.dp),
                //(colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = !isLoading
            ) {
                Text("Confirm", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

suspend fun getAddressFromLatLng(latitude: Double, longitude: Double): String {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val url = "https://nominatim.openstreetmap.org/reverse?lat=$latitude&lon=$longitude&format=json"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "QuickWorkApp/1.0")
            .build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val json = response.body?.string() ?: throw Exception("Empty response")
            val jsonObject = JSONObject(json)
            if (jsonObject.has("error")) {
                throw Exception(jsonObject.getString("error"))
            }
            jsonObject.optString("display_name", "Unknown location")
        } else {
            throw Exception("HTTP ${response.code}")
        }
    }
}