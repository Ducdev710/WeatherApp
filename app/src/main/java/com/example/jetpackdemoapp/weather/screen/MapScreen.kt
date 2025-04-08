package com.example.jetpackdemoapp.weather.screen

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.jetpackdemoapp.R
import com.example.jetpackdemoapp.weather.viewModel.WeatherViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


@SuppressLint("UseCompatLoadingForDrawables")
@Composable
fun MapScreen(
    latitude: Double,
    longitude: Double,
    cityName: String?,
    currentTemp: Int?,
    temperatureUnit: WeatherViewModel.TemperatureUnit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Configure osmdroid
    Configuration.getInstance().load(context, androidx.preference.PreferenceManager.getDefaultSharedPreferences(context))

    // Create the map view
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
        }
    }

    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map view
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                // Center the map on the location
                val position = GeoPoint(latitude, longitude)
                map.controller.setZoom(15.0)
                map.controller.setCenter(position)

                // Add a marker for current location
                val marker = Marker(map).apply {
                    setPosition(position)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = cityName ?: "Current Location" // Use city name if available

                    // Display current temperature in the snippet
                    val tempDisplay = if (currentTemp != null) {
                        val unitSuffix = if (temperatureUnit == WeatherViewModel.TemperatureUnit.CELSIUS) "°C" else "°F"
                        "Current temperature: $currentTemp$unitSuffix"
                    } else {
                        "Weather data unavailable"
                    }
                    snippet = tempDisplay

                    // Custom icon
                    val icon = context.resources.getDrawable(R.drawable.baseline_location_pin_24, null)
                    icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
                    setIcon(icon)
                }
                map.overlays.clear()
                map.overlays.add(marker)
            }
        )

        // Close button (X) at top right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(48.dp)
                    .background(color = Color.White.copy(alpha = 0.7f), shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Map",
                    tint = Color.Black
                )
            }
        }
    }
}