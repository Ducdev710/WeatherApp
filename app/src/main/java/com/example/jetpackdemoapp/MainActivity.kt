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
import com.example.jetpackdemoapp.data.model.service.RetrofitInstance
import com.example.jetpackdemoapp.notification.WeatherWorkScheduler
import com.example.jetpackdemoapp.ui.theme.JetpackDemoAppTheme
import com.example.jetpackdemoapp.weather.screen.WeatherScreen
import com.example.jetpackdemoapp.weather.viewModel.WeatherViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var location: Location? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use the regular scheduler for production
        WeatherWorkScheduler.scheduleDailyWeatherNotification(this)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            JetpackDemoAppTheme {
                var locationState by remember { mutableStateOf<Location?>(null) }

                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        getLastLocation { loc ->
                            locationState = loc
                        }
                    }
                }
                locationState?.let {
                    WeatherScreen(it.latitude, it.longitude)
                }

                /*Column {
                    // Button row for testing notifications
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Test immediate notification
                        Button(
                            onClick = { sendTestNotification() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Now")
                        }

                        // Test 5-minute periodic notifications
                        Button(
                            onClick = {
                                testShortIntervalNotification()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test 5min")
                        }
                    }

                    locationState?.let {
                        WeatherScreen(it.latitude, it.longitude)
                    }
                }*/
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getLastLocation { loc ->
                location = loc
            }
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.d("MainActivity", "Notification permission denied")
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation(onLocationReceived: (Location) -> Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                // Save location for notifications
                val weatherViewModel = ViewModelProvider(
                    this,
                    WeatherViewModel.Factory(RetrofitInstance.weatherRepository, "7a85369669e56294a7779bb9f8be8563")
                )[WeatherViewModel::class.java]

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
}