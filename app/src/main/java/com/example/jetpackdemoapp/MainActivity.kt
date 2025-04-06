package com.example.jetpackdemoapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.jetpackdemoapp.data.model.service.RetrofitInstance
import com.example.jetpackdemoapp.notification.WeatherNotificationWorker
import com.example.jetpackdemoapp.notification.WeatherWorkScheduler
import com.example.jetpackdemoapp.ui.theme.JetpackDemoAppTheme
import com.example.jetpackdemoapp.weather.screen.WeatherScreen
import com.example.jetpackdemoapp.weather.viewModel.WeatherViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var location: Location? = null
    private lateinit var weatherViewModel: WeatherViewModel

    // Permission state constants
    private enum class PermissionState {
        INITIAL,
        NOTIFICATION_GRANTED,
        LOCATION_REQUESTED,
        ALL_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /// Initialize ViewModel
        weatherViewModel = ViewModelProvider(
            this,
            WeatherViewModel.Factory(
                repository = RetrofitInstance.getWeatherRepository(this),
                apiKey = "7a85369669e56294a7779bb9f8be8563"
            )
        )[WeatherViewModel::class.java]

        // Use the regular scheduler for production
        WeatherWorkScheduler.scheduleDailyWeatherNotification(this)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Restore permission state
        val permissionState = restorePermissionState()

        // Based on current permission state, request appropriate permissions
        when (permissionState) {
            PermissionState.INITIAL -> {
                requestNotificationPermission()
            }
            PermissionState.NOTIFICATION_GRANTED -> {
                requestLocationPermission()
            }
            PermissionState.LOCATION_REQUESTED, PermissionState.ALL_GRANTED -> {
                // Continue with location if granted
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    saveCurrentLocation()
                }
            }
        }

        setContent {
            JetpackDemoAppTheme {
                var locationState by remember { mutableStateOf<Location?>(null) }

                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        getLastLocation { loc ->
                            locationState = loc
                        }
                    }
                }

                Column {
                    locationState?.let {
                        WeatherScreen(it.latitude, it.longitude)
                    }
                }

//                // Add a debug button to your MainActivity UI to show saved location
//                Column {
//                    Row(
//                        modifier = Modifier.padding(16.dp),
//                        horizontalArrangement = Arrangement.spacedBy(8.dp)
//                    ) {
//                        // Test immediate notification
//                        Button(
//                            onClick = { sendTestNotification() },
//                            modifier = Modifier.weight(1f)
//                        ) {
//                            Text("Test Now")
//                        }
//
//                        // Check saved location
//                        Button(
//                            onClick = { checkSavedLocation() },
//                            modifier = Modifier.weight(1f)
//                        ) {
//                            Text("Check Location")
//                        }
//                    }
//
//                    locationState?.let {
//                        WeatherScreen(it.latitude, it.longitude)
//                    }
//                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        saveCurrentLocation()
        // Also adjust the notification title in the WeatherNotificationWorker
        getSharedPreferences("worker_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("notification_title", "Today's Weather Forecast")
            .apply()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                savePermissionState(PermissionState.INITIAL)
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                savePermissionState(PermissionState.NOTIFICATION_GRANTED)
                requestLocationPermission()
            }
        } else {
            // No notification permission needed on older Android versions
            savePermissionState(PermissionState.NOTIFICATION_GRANTED)
            requestLocationPermission()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            savePermissionState(PermissionState.LOCATION_REQUESTED)
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            savePermissionState(PermissionState.ALL_GRANTED)
            getLastLocation { loc ->
                location = loc
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            savePermissionState(PermissionState.ALL_GRANTED)
            getLastLocation { loc ->
                location = loc

                // Force refresh the UI with the new location
                runOnUiThread {
                    setContent {
                        JetpackDemoAppTheme {
                            WeatherScreen(loc.latitude, loc.longitude)
                        }
                    }
                }

                // Save for notifications
                val sharedPrefs = getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
                sharedPrefs.edit()
                    .putFloat("saved_latitude", loc.latitude.toFloat())
                    .putFloat("saved_longitude", loc.longitude.toFloat())
                    .apply()

                Log.d("MainActivity", "Permission granted and location loaded: ${loc.latitude}, ${loc.longitude}")
            }
        } else {
            // Handle permission denied
            Log.d("MainActivity", "Location permission denied")

            // Show a message to the user
            Toast.makeText(
                this,
                "Location permission is required to show weather data",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
            savePermissionState(PermissionState.NOTIFICATION_GRANTED)
            requestLocationPermission()
        } else {
            Log.d("MainActivity", "Notification permission denied")
        }
    }


    @SuppressLint("MissingPermission")
    private fun getLastLocation(onLocationReceived: (Location) -> Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                // Save location for notifications
                weatherViewModel.saveLocationForNotifications(this)
                onLocationReceived(it)
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendTestNotification() {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, "weather_notifications")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use an existing icon for testing
            .setContentTitle("Weather Test")
            .setContentText("This is a direct test notification")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(100, notification)
            Log.d("MainActivity", "Direct test notification sent")
        } catch (e: SecurityException) {
            Log.e("MainActivity", "No notification permission: ${e.message}", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val name = "Weather Notifications"
        val descriptionText = "Daily weather updates"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("weather_notifications", name, importance).apply {
            description = descriptionText
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        Log.d("MainActivity", "Notification channel created")
    }
    private fun testShortIntervalNotification() {
        WeatherWorkScheduler.scheduleDailyWeatherNotificationForTesting(this)
        Toast.makeText(
            this,
            "Scheduled test notification for 1 minute from now",
            Toast.LENGTH_LONG
        ).show()
    }
    @SuppressLint("MissingPermission")
    private fun saveCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val sharedPrefs = getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
                    sharedPrefs.edit()
                        .putFloat("saved_latitude", it.latitude.toFloat())
                        .putFloat("saved_longitude", it.longitude.toFloat())
                        .apply()

                    Log.d("MainActivity", "Saved location: ${it.latitude}, ${it.longitude}")
                }
            }
        }
    }
    private fun checkSavedLocation() {
        val sharedPrefs = getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        val lat = sharedPrefs.getFloat("saved_latitude", 0f)
        val lon = sharedPrefs.getFloat("saved_longitude", 0f)

        Toast.makeText(
            this,
            "Saved location: $lat, $lon",
            Toast.LENGTH_LONG
        ).show()

        Log.d("MainActivity", "Testing weather notification with saved location: $lat, $lon")

        // Create data with isTest flag to avoid any location permission issues
        val inputData = androidx.work.Data.Builder()
            .putBoolean("isTest", false) // Requesting real weather data
            .build()

        val workRequest = OneTimeWorkRequestBuilder<WeatherNotificationWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)

        Toast.makeText(
            this,
            "Weather notification requested, check notification area",
            Toast.LENGTH_SHORT
        ).show()

        // For additional debugging, check API key validity
        val apiKeyPrefix = if (WeatherNotificationWorker.API_KEY.length > 5)
            WeatherNotificationWorker.API_KEY.substring(0, 5) + "..."
        else "invalid"
        Log.d("MainActivity", "Weather API key prefix: $apiKeyPrefix")
    }
    // Permission state management methods
    private fun savePermissionState(state: PermissionState) {
        getSharedPreferences("permissions_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("permission_flow_state", state.name)
            .apply()
    }

    private fun restorePermissionState(): PermissionState {
        val savedState = getSharedPreferences("permissions_prefs", Context.MODE_PRIVATE)
            .getString("permission_flow_state", PermissionState.INITIAL.name)
        return try {
            PermissionState.valueOf(savedState ?: PermissionState.INITIAL.name)
        } catch (e: Exception) {
            PermissionState.INITIAL
        }
    }
}